// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cloudformation

import software.amazon.awssdk.services.lambda.model.Runtime
import software.aws.toolkits.jetbrains.services.lambda.LambdaPackager
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

interface Function : Resource, Exportable {
    fun codeLocation(): String
    fun setCodeLocation(location: String)
    fun runtime(): String
    fun handler(): String
}

const val LAMBDA_FUNCTION_TYPE = "AWS::Lambda::Function"
class LambdaFunction(private val delegate: Resource) : Resource by delegate, Function {
    override fun setCodeLocation(location: String) {
        setScalarProperty("Code", location)
    }

    override fun codeLocation(): String = getScalarProperty("Code")

    override fun runtime(): String = getScalarProperty("Runtime")

    override fun handler(): String = getScalarProperty("Handler")

    override fun createExporter(): ExportableResource = object: ExportableResource(this, "Code") {
        override fun updateResource(s3Location: String) {
            TODO("not implemented")
        }
    }

    override fun toString(): String = logicalName
}

const val SERVERLESS_FUNCTION_TYPE = "AWS::Serverless::Function"
class SamFunction(private val delegate: Resource) : Resource by delegate, Function {
    override fun setCodeLocation(location: String) {
        setScalarProperty("CodeUri", location)
    }

    override fun codeLocation(): String = getScalarProperty("CodeUri")

    override fun runtime(): String = getScalarProperty("Runtime")

    override fun handler(): String = getScalarProperty("Handler")

    override fun createExporter(): ExportableResource = object: ExportableResource(this, "CodeUri") {
        override fun packageResource(): CompletableFuture<Path> {
            
        }

        override fun updateResource(s3Location: String) {
            delegate.setMappingProperty("CodeUri", mapOf(
                "S3Bucket" to s3Location,
                "S3Key" to s3Location,
                "S3ObjectVersion" to s3Location
            ))
        }
    }

    override fun toString(): String = logicalName
}

val RESOURCE_MAPPINGS = mapOf<String, (Resource) -> Resource>(
    LAMBDA_FUNCTION_TYPE to ::LambdaFunction,
    SERVERLESS_FUNCTION_TYPE to ::SamFunction
)