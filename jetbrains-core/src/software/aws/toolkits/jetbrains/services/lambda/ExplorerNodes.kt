// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.lambda

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import icons.AwsIcons
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest
import software.aws.toolkits.jetbrains.core.AwsClientManager
import software.aws.toolkits.jetbrains.core.credentials.ProjectAccountSettingsManager
import software.aws.toolkits.jetbrains.core.explorer.AwsExplorerNode
import software.aws.toolkits.jetbrains.core.explorer.AwsExplorerResourceNode
import software.aws.toolkits.jetbrains.core.explorer.AwsExplorerServiceRootNode
import software.aws.toolkits.jetbrains.core.explorer.AwsTruncatedResultNode
import software.aws.toolkits.resources.message

class LambdaServiceNode(project: Project) : AwsExplorerServiceRootNode(project, message("lambda.service_name")) {
    override fun serviceName() = LambdaClient.SERVICE_NAME

    private val client: LambdaClient = AwsClientManager.getInstance(project).getClient()

    override fun loadResources(paginationToken: String?): Collection<AwsExplorerNode<*>> {
        val request = ListFunctionsRequest.builder()
        paginationToken?.let { request.marker(paginationToken) }

        val response = client.listFunctions(request.build())
        val resources: MutableList<AwsExplorerNode<*>> =
            response.functions().asSequence().sortedBy { it.functionName().toLowerCase() }.map { mapResourceToNode(it) }.toMutableList()
        response.nextMarker()?.let {
            resources.add(AwsTruncatedResultNode(this, it))
        }

        return resources
    }

    private fun mapResourceToNode(resource: FunctionConfiguration) =
        LambdaFunctionNode(project!!, client, this, resource)
}

class LambdaFunctionNode(
    project: Project,
    val client: LambdaClient,
    serviceNode: LambdaServiceNode,
    functionConfiguration: FunctionConfiguration
) : AwsExplorerResourceNode<FunctionConfiguration>(project, serviceNode, functionConfiguration, AwsIcons.Resources.LAMBDA_FUNCTION) {
    private val accountSettingsManager = ProjectAccountSettingsManager.getInstance(project)

    val function = functionConfiguration.toDataClass(accountSettingsManager.activeCredentialProvider.id, accountSettingsManager.activeRegion)

    override fun getChildren(): Collection<AbstractTreeNode<Any>> = emptyList()

    override fun resourceType(): String = "function"

    override fun toString(): String = functionName()

    fun functionName(): String = function.name

    fun handlerPsi(): Array<NavigatablePsiElement> = Lambda.findPsiElementsForHandler(
        super.getProject()!!,
        function.runtime,
        function.handler
    )
}