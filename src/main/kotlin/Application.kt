package com.example

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Producto(val id: Int, val nombre: String, val descripcion: String)

@Serializable
data class ProductoInput(val nombre: String, val descripcion: String)

val productos = mutableListOf(
    Producto(id = 1, nombre = "Laptop", descripcion = "Ultrabook 13 pulgadas"),
    Producto(id = 2, nombre = "Smartphone", descripcion = "Pantalla OLED 6.1''"),
    Producto(id = 3, nombre = "Teclado", descripcion = "Mecánico retroiluminado"),
    Producto(id = 4, nombre = "Mouse", descripcion = "Ergonómico inalámbrico"),
    Producto(id = 5, nombre = "Monitor", descripcion = "4K UHD 27 pulgadas")
)
var ultimoId = productos.maxOf { it.id }

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::modulo).start(wait = true)
}

fun Application.modulo() {
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
            call.respond(productos)
        }

        get("/productos/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                val item = productos.find { it.id == id }
                if (item != null) {
                    call.respond(item)
                } else {
                    call.respond(HttpStatusCode.NotFound, "No existe")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID no válido")
            }
        }

        post("/productos") {
            val datos = call.receive<ProductoInput>()
            val nuevo = Producto(id = ++ultimoId, nombre = datos.nombre, descripcion = datos.descripcion)
            productos.add(nuevo)
            call.respond(HttpStatusCode.Created, nuevo)
        }

        put("/productos/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                val actual = productos.find { it.id == id }
                if (actual != null) {
                    val entrada = call.receive<ProductoInput>()
                    val actualizado = Producto(id = id, nombre = entrada.nombre, descripcion = entrada.descripcion)
                    productos[productos.indexOf(actual)] = actualizado
                    call.respond(actualizado)
                } else {
                    call.respond(HttpStatusCode.NotFound, "No encontrado")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
            }
        }

        delete("/productos/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id != null) {
                val item = productos.find { it.id == id }
                if (item != null) {
                    productos.remove(item)
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, "No existe")
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, "ID inválido")
            }
        }
    }
}
