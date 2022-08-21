/**
 *   ownCloud Android client application
 *
 *   @author Abel García de Prada
 *   Copyright (C) 2020 ownCloud GmbH.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.uteknoid.drive.data.roommigrations

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.uteknoid.drive.data.OwncloudDatabase
import org.junit.Rule

open class MigrationTest {
    @Rule
    @JvmField
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OwncloudDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    fun getCount(db: SupportSQLiteDatabase, tableName: String): Long =
        db.compileStatement("SELECT COUNT(*) FROM `$tableName`").simpleQueryForLong()

    fun performMigrationTest(
        previousVersion: Int,
        currentVersion: Int,
        insertData: (SupportSQLiteDatabase) -> Unit,
        recoverPreviousData: Boolean = true,
        validateMigration: (SupportSQLiteDatabase) -> Unit,
        listOfMigrations: Array<Migration>
    ) {
        helper.createDatabase(TEST_DB_NAME, previousVersion).run {
            if (recoverPreviousData) insertData(this)
        }

        helper.runMigrationsAndValidate(
            TEST_DB_NAME, currentVersion, true, *listOfMigrations
        ).also { validateMigration(it) }
    }

    companion object {
        const val TEST_DB_NAME = "migration-test"

        const val DB_VERSION_27 = 27
        const val DB_VERSION_28 = 28
        const val DB_VERSION_29 = 29
        const val DB_VERSION_30 = 30
        const val DB_VERSION_31 = 31
        const val DB_VERSION_32 = 32
        const val DB_VERSION_33 = 33
        const val DB_VERSION_34 = 34
        const val DB_VERSION_35 = 35
        const val DB_VERSION_36 = 36

    }
}
