package ru.lesnoy40rt77.weatheradvisor.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.lesnoy40rt77.weatheradvisor.data.CityRepository

@RunWith(AndroidJUnit4::class)
class CityDatabaseInstrumentedTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DATABASE_NAME)

        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DATABASE_NAME
        )
            .createFromAsset("database/cities.db")
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun prepackagedDatabaseIsAvailableAndNotEmpty() = runBlocking {
        val count = database.cityDao().getCount()

        assertTrue("База городов должна содержать данные", count > 10_000)
    }

    @Test
    fun cityRepositorySearchFindsMoscow() = runBlocking {
        val repository = CityRepository(database.cityDao())

        val results = repository.searchCities("моск")

        assertTrue("Поиск по Москве должен вернуть результаты", results.isNotEmpty())
        assertTrue(
            "В результатах должна быть Москва",
            results.any { city ->
                city.countryCode == "RU" && city.name.contains("Москва", ignoreCase = true)
            }
        )
    }

    private companion object {
        const val TEST_DATABASE_NAME = "weather_advisor_test.db"
    }
}
