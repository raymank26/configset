package com.configset.dashboard

import com.google.common.io.Resources
import com.hubspot.jinjava.Jinjava
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class TemplateRenderer(private val templatesFilePath: String?) {

    private val jinjava = Jinjava()

    init {
        jinjava.setResourceLocator { fullName, _, _ -> loadTemplate(fullName) }
    }


    fun render(templateName: String, params: Map<String, Any> = emptyMap()): String {
        return jinjava.render(loadTemplate(templateName), params)
    }

    private fun loadTemplate(name: String): String {
        return if (templatesFilePath != null) {
            Files.readString(Path.of(templatesFilePath, name))
        } else {
            Resources.toString(Resources.getResource("templates/$name"), StandardCharsets.UTF_8)
        }
    }
}