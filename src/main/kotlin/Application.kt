package com.example

import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Categoria(val id: Int, val nombre: String)

@Serializable
data class Producto(val id: Int, val nombre: String, val descripcion: String, val categoriaId: Int)

@Serializable
data class ProductoInput(val nombre: String, val descripcion: String, val categoriaId: Int)

val categorias = mutableListOf(
    Categoria(1, "Electrónica"),
    Categoria(2, "Periféricos"),
    Categoria(3, "Hogar")
)

val productos = mutableListOf(
    Producto(1, "Laptop", "Ultrabook 13 pulgadas", 1),
    Producto(2, "Smartphone", "Pantalla OLED 6.1''", 1),
    Producto(3, "Teclado", "Mecánico retroiluminado", 2),
    Producto(4, "Mouse", "Ergonómico inalámbrico", 2),
    Producto(5, "Aspiradora", "Robot aspirador", 3)
)

var ultimoId = productos.maxOf { it.id }

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    routing {
        get("/") {
            call.respondText("Servidor activo", ContentType.Text.Plain)
        }

        get("/productos") {
            val categoriaId = call.request.queryParameters["categoriaId"]?.toIntOrNull()
            val resultado = if (categoriaId != null) {
                productos.filter { it.categoriaId == categoriaId }
            } else {
                productos
            }
            call.respond(resultado)
        }


        get("/productos/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val producto = productos.find { it.id == id }
            if (producto != null) {
                call.respond(producto)
            } else {
                call.respond(HttpStatusCode.NotFound, "Producto no encontrado")
            }
        }

        get("/productos/categoria/{categoriaId}") {
            val categoriaId = call.parameters["categoriaId"]?.toIntOrNull()
            if (categoriaId != null) {
                val filtrados = productos.filter { it.categoriaId == categoriaId }
                call.respond(filtrados)
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID de categoría inválido")
            }
        }
        
        post("/productos") {
            val input = call.receive<ProductoInput>()
            val nuevo = Producto(++ultimoId, input.nombre, input.descripcion, input.categoriaId)
            productos.add(nuevo)
            call.respond(HttpStatusCode.Created, nuevo)
        }

        // Actualizar producto
        put("/productos/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                val existente = productos.find { it.id == id }
                if (existente != null) {
                    val entrada = call.receive<ProductoInput>()
                    val actualizado = Producto(id, entrada.nombre, entrada.descripcion, entrada.categoriaId)
                    productos[productos.indexOf(existente)] = actualizado
                    call.respond(actualizado)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Producto no encontrado")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
            }
        }

        // Eliminar producto
        delete("/productos/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            val producto = productos.find { it.id == id }
            if (producto != null) {
                productos.remove(producto)
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Producto no encontrado")
            }
        }

        // Listar categorías
        get("/categorias") {
            call.respond(categorias)
        }
    }
}
