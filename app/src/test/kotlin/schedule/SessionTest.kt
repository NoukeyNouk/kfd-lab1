package schedule

import java.time.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionTest {

    @Test
    fun test01_cannotCreateSessionWithEmptyOrBlankTopic() {
        val date = LocalDate.of(2026, 10, 15)
        assertFailsWith<IllegalArgumentException> {
            Lecture(date, "", "Room 101", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Practice(date, "   ", "Room 101", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Exam(date, "	", "Room 101", listOf("Teacher A", "Teacher B"))
        }
        assertFailsWith<IllegalArgumentException> {
            Cancelled(date, "")
        }
    }

    @Test
    fun test02_cannotCreateSessionWithInvalidYear() {
        val dateZero = LocalDate.of(0, 1, 1)
        val dateTooBig = LocalDate.of(10000, 1, 1)

        assertFailsWith<IllegalArgumentException> {
            Lecture(dateZero, "Topic", "Room 101", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Practice(dateTooBig, "Topic", "Room 101", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Exam(dateZero, "Topic", "Room 101", listOf("Teacher A", "Teacher B"))
        }
        assertFailsWith<IllegalArgumentException> {
            Cancelled(dateTooBig, "Topic")
        }
    }

    @Test
    fun test03_cannotCreateActiveSessionWithoutTeachers() {
        val date = LocalDate.of(2026, 10, 15)
        assertFailsWith<IllegalArgumentException> {
            Lecture(date, "Topic", "Room 101", emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            Practice(date, "Topic", "Room 101", emptyList())
        }
    }

    @Test
    fun test04_cannotCreateExamWithLessThanTwoTeachers() {
        val date = LocalDate.of(2026, 10, 15)
        assertFailsWith<IllegalArgumentException> {
            Exam(date, "Exam Topic", "Room 101", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Exam(date, "Exam Topic", "Room 101", emptyList())
        }
    }

    @Test
    fun test05_cannotCreateExamWithDuplicateTeachers() {
        val date = LocalDate.of(2026, 10, 15)
        assertFailsWith<IllegalArgumentException> {
            Exam(date, "Exam Topic", "Room 101", listOf("Teacher A", "Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Lecture(date, "Topic", "Room 101", listOf("Teacher A", "Teacher A"))
        }
    }

    @Test
    fun test06_mutatingOriginalListDoesNotAffectSession() {
        val names = mutableListOf("Teacher A", "Teacher B")
        val exam = Exam(LocalDate.of(2026, 10, 15), "Exam Topic", "Room 101", names)
        names.clear()
        assertEquals(2, exam.teachers.size)
        assertEquals(listOf("Teacher A", "Teacher B"), exam.teachers)
    }

    @Test
    fun test07_cannotMutateSessionThroughReturnedTeachersList() {
        val exam = Exam(LocalDate.of(2026, 10, 15), "Exam Topic", "Room 101", listOf("Teacher A", "Teacher B"))
        assertFailsWith<UnsupportedOperationException> {
            (exam.teachers as MutableList<String>).add("Teacher C")
        }
    }

    @Test
    fun test08_copyMethodIsNotExposedOnRegularClasses() {
        val methods = Lecture::class.java.declaredMethods.map { it.name }
        assertFalse(methods.contains("copy"))
    }

    @Test
    fun test09_cannotCreateSessionWithOuterWhitespaceOrControlChars() {
        val date = LocalDate.of(2026, 10, 15)
        assertFailsWith<IllegalArgumentException> {
            Lecture(date, " Topic", "Room 101", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Lecture(date, "Topic ", "Room 101", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Lecture(date, "Top\u0001ic", "Room 101", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Lecture(date, "Topic", " Room 101", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Lecture(date, "Topic", "Room 101", listOf(" Teacher A"))
        }
    }

    @Test
    fun test10_cannotCreateActiveSessionWithDashLocation() {
        val date = LocalDate.of(2026, 10, 15)
        assertFailsWith<IllegalArgumentException> {
            Lecture(date, "Topic", "-", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Practice(date, "Topic", "-", listOf("Teacher A"))
        }
        assertFailsWith<IllegalArgumentException> {
            Exam(date, "Topic", "-", listOf("Teacher A", "Teacher B"))
        }
    }

    @Test
    fun test11_cannotCreateActiveSessionWithDashTeacher() {
        val date = LocalDate.of(2026, 10, 15)
        assertFailsWith<IllegalArgumentException> {
            Lecture(date, "Topic", "Room 101", listOf("-"))
        }
        assertFailsWith<IllegalArgumentException> {
            Exam(date, "Topic", "Room 101", listOf("Teacher A", "-"))
        }
    }

    @Test
    fun test12_caseSensitiveTeachersAreAllowed() {
        val date = LocalDate.of(2026, 10, 15)
        val exam = Exam(date, "Topic", "Room 101", listOf("Ivanov", "ivanov"))
        assertEquals(2, exam.teachers.size)
    }

    @Test
    fun test13_allFourTypesCreatedSuccessfully() {
        val date = LocalDate.of(2026, 10, 15)
        val l = Lecture(date, "Topic L", "Room 1", listOf("T1"))
        val p = Practice(date, "Topic P", "Room 2", listOf("T2"))
        val e = Exam(date, "Topic E", "Room 3", listOf("T1", "T2"))
        val c = Cancelled(date, "Topic C")

        assertEquals("Topic L", l.topic)
        assertEquals("Topic P", p.topic)
        assertEquals("Topic E", e.topic)
        assertEquals("Topic C", c.topic)
    }

    @Test
    fun test14_repositoryStoresSessionsAndReturnsSnapshots() {
        val repo = delegate()
        val s1 = Cancelled(LocalDate.of(2026, 10, 15), "Topic 1")
        val s2 = Cancelled(LocalDate.of(2026, 10, 16), "Topic 2")

        repo.add(s1)
        val snapshot1 = repo.all()
        assertEquals(1, snapshot1.size)

        repo.add(s2)
        assertEquals(1, snapshot1.size)
        assertEquals(2, repo.all().size)
    }

    @Test
    fun test15_auditedRepositoryLogsOperationsWithoutDoubleCounting() {
        val backend = delegate()
        val audited = AuditedSessionRepository(backend)

        val s1 = Cancelled(LocalDate.of(2026, 10, 15), "Topic 1")
        val s2 = Cancelled(LocalDate.of(2026, 10, 16), "Topic 2")
        val s3 = Cancelled(LocalDate.of(2026, 10, 17), "Topic 3")

        audited.add(s1)
        audited.addAll(listOf(s2, s3))
        audited.addAll(emptyList())

        assertEquals(3, audited.all().size)
        val events = audited.audits()
        assertEquals(3, events.size)
        assertEquals(AuditEvent("add", 1), events[0])
        assertEquals(AuditEvent("addAll", 2), events[1])
        assertEquals(AuditEvent("addAll", 0), events[2])
    }

    @Test
    fun test16_parserEnforcesValidationPriorities() {
        val res1 = parseSessionLine(1, "2026-10-05;LECTURE;Topic;Room 1;Ivanov")
        assertTrue(res1 is ParseResult.Failure)
        assertEquals("invalid field separator", formatReason(res1.reason))

        val res2 = parseSessionLine(2, "2026-10-05|LECTURE|Topic|Room 1|Ivanov|extra")
        assertTrue(res2 is ParseResult.Failure)
        assertEquals("got 6 fields, expected 5", formatReason(res2.reason))

        val res3 = parseSessionLine(3, "2026-13-45|SEMINAR|Topic|Room 1|Ivanov")
        assertTrue(res3 is ParseResult.Failure)
        assertEquals("invalid date \"2026-13-45\"", formatReason(res3.reason))

        val res4 = parseSessionLine(4, "2026-10-05|SEMINAR|Topic|Room 1|Ivanov")
        assertTrue(res4 is ParseResult.Failure)
        assertEquals("unknown session type \"SEMINAR\"", formatReason(res4.reason))

        val res5 = parseSessionLine(5, "2026-10-05|EXAM|Topic|Room 1|Ivanov")
        assertTrue(res5 is ParseResult.Failure)
        assertEquals("type \"EXAM\" requires at least 2 teachers, got 1", formatReason(res5.reason))
    }
}
