package com.example.todo

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.Priority
import model.Task
import model.TaskRepository
import model.tasksAsTable

fun Application.configureRouting() {
    install(StatusPages) {
        exception<IllegalStateException> { call, cause ->
            call.respondText("App in illegal state as ${cause.message}")
        }
    }

    routing {
        staticResources("/task-ui", "task-ui")

        get("/") {
            call.respondText("Hello World!")
        }

        get("/test1") {
            val text = "<h1>Hello From Ktor</h1>"
            val type = ContentType.parse("text/html")
            call.respondText(text, type)
        }

        get("/error-test") {
            throw IllegalStateException("Too Busy")
        }

        get("/tasks") {
            call.respondText(
                contentType = ContentType.parse("text/html"),
                text = TaskRepository.allTasks().tasksAsTable()
            )
        }

        get("/tasks/byPriority/{priority?}") {
            val priorityAsText = call.parameters["priority"]
            if (priorityAsText == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            try {
                val priority = Priority.valueOf(priorityAsText)
                val tasks = TaskRepository.tasksByPriority(priority)

                if (tasks.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respondText(
                    contentType = ContentType.parse("text/html"),
                    text = tasks.tasksAsTable()
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        post("/tasks") {
            val formContent = call.receiveParameters()
            val params = Triple(
                formContent["name"] ?: "",
                formContent["description"] ?: "",
                formContent["priority"] ?: ""
            )

            if (params.toList().any { it.isEmpty() }) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            try {
                val priority = Priority.valueOf(params.third)
                val task = Task(params.first, params.second, priority)
                TaskRepository.addTask(task)
                call.respond(HttpStatusCode.NoContent)
            } catch (_: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest)
            } catch (_: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}
