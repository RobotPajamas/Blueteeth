package com.robotpajamas.blueteeth.models

import org.junit.Assert.*
import org.junit.Test


// TODO: Get JUnit5 or Spek in here for easier BDD
// Or use JUnit4 nested test runners
class ResultTest {
    @Test
    fun success_whenConstructed_shouldPopulateValue() {
        val expectedString = "hello"
        val resultString = Result.Success(expectedString)
        assertEquals(resultString.value, expectedString)

        val expectedInt = 42
        val resultInt = Result.Success(expectedInt)
        assertEquals(resultInt.value, expectedInt)

        val expectedDouble = 42.123
        val resultDouble = Result.Success(expectedDouble)
        assertEquals(resultDouble.value, expectedDouble, 0.0001)

        val expectedList = listOf(1, 2, 3, 4)
        val resultList = Result.Success(expectedList)
        assertArrayEquals(resultList.value.toIntArray(), expectedList.toIntArray())

        val expectedObject = object {
            val x: Int = 56
            var y: String = "asdasd"
        }
        val resultObject = Result.Success(expectedObject)
        assertEquals(resultObject.value, expectedObject) // Just checks references, not fields
    }

    @Test
    fun success_whenConstructed_shouldNotPopulateError() {
        val result = Result.Success(0)
        assertNull(result.error)
    }

    @Test
    fun failure_whenConstructed_shouldNotPopulateValue() {
        val exception = NullPointerException()
        val result = Result.Failure(exception)
        assertNull(result.value)
    }

    @Test
    fun failure_whenConstructed_shouldPopulateError() {
        val exception = NullPointerException()
        val result = Result.Failure(exception)
        assertEquals(result.error, exception)
    }

    @Test
    fun isSuccess_whenSuccessCreated_shouldReturnTrue() {
        val result = Result.Success(0)
        assertTrue(result.isSuccess)
    }

    @Test
    fun isFailure_whenSuccessCreated_shouldReturnFalse() {
        val result = Result.Success(0)
        assertFalse(result.isFailure)
    }

    @Test
    fun isSuccess_whenFailureCreated_shouldReturnFalse() {
        val result = Result.Failure(NullPointerException())
        assertFalse(result.isSuccess)
    }

    @Test
    fun isFailure_whenFailureCreated_shouldReturnTrue() {
        val result = Result.Failure(NullPointerException())
        assertTrue(result.isFailure)
    }

    @Test
    fun unwrap_whenSuccessCreated_shouldReturnTrue() {
        val result = Result.Success(0)
        assertEquals(result.unwrap(), 0)
    }

    @Test(expected = NullPointerException::class)
    fun unwrap_whenFailureCreated_shouldThrowException() {
        val exception = NullPointerException()
        val result = Result.Failure(exception)
        result.unwrap()
    }
}