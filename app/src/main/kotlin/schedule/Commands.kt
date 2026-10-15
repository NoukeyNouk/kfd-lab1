package schedule

interface CommandHandlers {
    fun period(command: Command.Period): Int
    fun byType(command: Command.ByType): Int
    fun errors(command: Command.Errors): Int
}

object StudentCommandHandlers : CommandHandlers {
    override fun period(command: Command.Period): Int = runPeriod(command)
    override fun byType(command: Command.ByType): Int = runType(command)
    override fun errors(command: Command.Errors): Int = runErrors(command)
}

fun runPeriod(command: Command.Period): Int {
    var hasErrors = false
    var totalSelected = 0
    forEachScheduleLine(command.file) { lineNumber, line ->
        when (val result = parseSessionLine(lineNumber, line)) {
            is ParseResult.Failure -> hasErrors = true
            is ParseResult.Success -> {
                val session = result.session
                if (session.date in command.from..command.to) {
                    val location = when (session) {
                        is Lecture -> session.location
                        is Practice -> session.location
                        is Exam -> session.location
                        is Cancelled -> "-"
                        else -> error("Unknown session: $session")
                    }
                    val typeName = when (session) {
                        is Lecture -> "LECTURE"
                        is Practice -> "PRACTICE"
                        is Exam -> "EXAM"
                        is Cancelled -> "CANCELLED"
                        else -> error("Unknown session: $session")
                    }
                    println("${session.date} | $typeName | ${session.topic} | $location")
                    totalSelected++
                }
            }
        }
    }
    println("Total: $totalSelected")
    return if (hasErrors) 1 else 0
}

fun runType(command: Command.ByType): Int {
    var hasErrors = false
    var totalSelected = 0
    forEachScheduleLine(command.file) { lineNumber, line ->
        when (val result = parseSessionLine(lineNumber, line)) {
            is ParseResult.Failure -> hasErrors = true
            is ParseResult.Success -> {
                val session = result.session
                val matches = when (command.type) {
                    SessionType.LECTURE -> session is Lecture
                    SessionType.PRACTICE -> session is Practice
                    SessionType.EXAM -> session is Exam
                    SessionType.CANCELLED -> session is Cancelled
                }
                if (matches) {
                    val location = when (session) {
                        is Lecture -> session.location
                        is Practice -> session.location
                        is Exam -> session.location
                        is Cancelled -> "-"
                        else -> error("Unknown session: $session")
                    }
                    val teachers = when (session) {
                        is Lecture -> session.teachers.joinToString(", ")
                        is Practice -> session.teachers.joinToString(", ")
                        is Exam -> session.teachers.joinToString(", ")
                        is Cancelled -> "-"
                        else -> error("Unknown session: $session")
                    }
                    println("${session.date} | ${command.type.name} | ${session.topic} | $location | $teachers")
                    totalSelected++
                }
            }
        }
    }
    println("Total: $totalSelected")
    return if (hasErrors) 1 else 0
}

fun runErrors(command: Command.Errors): Int {
    var rejectedCount = 0
    var totalLines = 0
    forEachScheduleLine(command.file) { lineNumber, line ->
        totalLines++
        val result = parseSessionLine(lineNumber, line)
        if (result is ParseResult.Failure) {
            rejectedCount++
            println(formatFailure(result))
        }
    }
    println("Rejected: $rejectedCount of $totalLines")
    return if (rejectedCount > 0) 1 else 0
}
