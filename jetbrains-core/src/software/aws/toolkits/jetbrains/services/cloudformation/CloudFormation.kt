// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudformation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.cloudformation.CloudFormationClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture

class CloudFormation() {
    fun createStack(
        cfnClient: CloudFormationClient,
        name: String,
        template: VirtualFile,
        assets: Map<String, File>
    ): String {
        return cfnClient.createStack {
            it.stackName(name)
//            it.templateBody(template)
        }.stackId()
    }

//    fun extractArtifactLocations(project: Project, template: VirtualFile) {
//        val template = CloudFormationTemplate.parse(project, template)
//        template.resources().filterIsInstance<Function>()
//            .filter { it.isLocal() }
//    }

    fun uploadArtifact(
        s3Client: S3Client,
        bucket: String,
        file: Path,
        progressIndicator: ProgressIndicator
    ): CompletableFuture<String> {
        val hash = calculateMd5(file)
        val key = "$hash${file.fileName}"

        // TODO: message()
        progressIndicator.text = "Uploading ${file.fileName}"
        progressIndicator.isIndeterminate = true

        val headObjectResponse = s3Client.headObject {
            it.bucket(bucket).key(key)
        }

        if (headObjectResponse != null) {
            progressIndicator.fraction = 1.0
            return CompletableFuture.completedFuture(key)
        }

        val por = PutObjectRequest.builder().bucket(bucket).key(key).build()
        val requestBody = RequestBody.fromInputStream(
            ProgressTrackingInputStream(file, progressIndicator),
            Files.size(file)
        )

        val future = CompletableFuture<String>()
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                s3Client.putObject(por, requestBody)
                future.complete(key)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    private class ProgressTrackingInputStream(
        file: Path,
        private val progressIndicator: ProgressIndicator
    ) : InputStream() {
        private val delegate = Files.newInputStream(file)
        private val length = Files.size(file)
        private var read = 0.0

        override fun read(): Int {
            progressIndicator.checkCanceled()

            return delegate.read().also {
                increment(1)
            }
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            progressIndicator.checkCanceled()

            return super.read(b, off, len).also {
                increment(it)
            }
        }

        private fun increment(amount: Int) {
            read += amount

            progressIndicator.fraction = read / length
        }
    }

    private fun calculateMd5(file: Path): Any {
        val digest = MessageDigest.getInstance("MD5")
        file.toFile().forEachBlock { buffer, _ ->
            digest.update(buffer)
        }
        return digest.digest()
    }
}