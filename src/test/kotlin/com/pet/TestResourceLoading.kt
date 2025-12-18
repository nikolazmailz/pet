package com.pet

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class TestResourceLoading {

    @Test
    fun `should load file from test resources`() {
        val resource = ClassPathResource("example.docx")

        // файл существует
        assertTrue(resource.exists(), "example.docx not found in test resources")

        val bytes = resource.inputStream.use { it.readBytes() }

        // файл не пустой
        assertTrue(bytes.isNotEmpty(), "Loaded file is empty")
    }

    @Test
    fun `should load file from classloader`() {
        val stream = this::class.java.classLoader
            .getResourceAsStream("example.docx")

        assertTrue(stream != null, "example.docx not found in classpath")

        val bytes = stream!!.readBytes()
        assertTrue(bytes.isNotEmpty(), "Loaded file is empty")
    }
}

