package com.asyncapi.plugin.idea._core

import com.asyncapi.plugin.idea._core.xpath.JsonFileXPath
import com.asyncapi.plugin.idea._core.xpath.YamlFileXPath
import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.components.Service
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile

@Service
class AsyncAPISchemaRecognizer {

    fun isSchema(file: PsiFile?, useIndex: Boolean = false): Boolean {
        file ?: return false

        if (file.fileType !is YAMLFileType && file.fileType !is JsonFileType) {
            return false
        }

        return if (useIndex) {
            /*
                TODO: implement.
             */
            false
        } else {
            "2.0.0" == extractAsyncAPIKey(file)
        }
    }

    private fun extractAsyncAPIKey(file: PsiFile?): String? {
        file ?: return null

        val psiXPath = "$.asyncapi"
        return when (file) {
            is JsonFile -> JsonFileXPath.findText(file as? JsonFile, psiXPath).firstOrNull()
            is YAMLFile -> YamlFileXPath.findText(file as? YAMLFile, psiXPath).firstOrNull()
            else -> null
        }
    }

}