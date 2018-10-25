// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudformation

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLLanguage
import software.amazon.awssdk.services.s3.S3Client
import software.aws.toolkits.jetbrains.services.cloudformation.yaml.YamlCloudFormationTemplate
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

interface CloudFormationTemplate {
    fun resources(): Sequence<Resource>

    fun getResourceByName(logicalName: String): Resource? = resources().firstOrNull { it.logicalName == logicalName }

    fun saveTo(file: File) {
        FileUtil.createIfNotExists(file)
        file.writeText(text())
    }

    fun text(): String

    companion object {
        fun parse(project: Project, templateFile: VirtualFile): CloudFormationTemplate {
            return when (templateFile.fileType) {
                YAMLFileType.YML -> YamlCloudFormationTemplate(project, templateFile)
                else -> throw UnsupportedOperationException("Only YAML CloudFormation templates are supported")
            }
        }

        fun convertPsiToResource(psiElement: PsiElement): Resource? {
            return when (psiElement.language) {
                YAMLLanguage.INSTANCE -> YamlCloudFormationTemplate.convertPsiToResource(psiElement)
                else -> throw UnsupportedOperationException("Only YAML CloudFormation templates are supported")
            }
        }
    }
}

enum class PropertyType {
    SCALAR, LIST, MAP
}

interface Resource {
    val logicalName: String

    fun isType(requestedType: String): Boolean
    fun type(): String?
    fun getPropertyType(key: String): PropertyType
    fun getScalarProperty(key: String): String
    fun setScalarProperty(key: String, value: String)
    fun setMappingProperty(key: String, value: Map<String, Any>)
}

interface Exportable {
    fun createExporter(): ExportableResource
}

abstract class ExportableResource(
    private val resource: Resource,
    private val exportProperty: String
) {
    fun isLocal() = resource.getPropertyType(exportProperty) == PropertyType.SCALAR &&
            !resource.getScalarProperty(exportProperty).startsWith("s3://")

    fun exportResource(s3Client: S3Client): CompletableFuture<Void> {
        TODO()
    }

    abstract fun packageResource(): CompletableFuture<Path>

    abstract fun updateResource(s3Location: String)

    fun createS3Url() {

    }

    fun createS3Mapping() {}
}