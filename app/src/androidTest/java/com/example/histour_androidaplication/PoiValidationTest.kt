package com.example.histour_androidaplication

import org.junit.Assert.*
import org.junit.Test

class PoiValidationTest {

    @Test
    fun nome_nao_deve_estar_vazio() {
        val nome = "Torre dos Clérigos"
        assertTrue(nome.isNotEmpty())
    }

    @Test
    fun descricao_deve_ter_mais_de_10_caracteres() {
        val descricao = "Monumento histórico no Porto."
        assertTrue(descricao.length >= 10)
    }

    @Test
    fun latitude_valida_retorna_true() {
        val latitude = 41.1496
        assertTrue(latitude in -90.0..90.0)
    }

    @Test
    fun longitude_valida_retorna_true() {
        val longitude = -8.6109
        assertTrue(longitude in -180.0..180.0)
    }

    @Test
    fun latitude_invalida_retorna_false() {
        val latitude = 100.0
        assertFalse(latitude in -90.0..90.0)
    }
}
