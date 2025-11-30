package io.pocketbase.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MigrationControllerTest {
    private val controller = MigrationController()

    private val promptsSchema = CollectionSchema(
        name = "s_prompts",
        fields = listOf(
            FieldSchema(
                name = "id",
                type = FieldType.TEXT,
                required = true
            ),
            FieldSchema(
                name = "text",
                type = FieldType.TEXT,
                required = true,
                constraints = FieldConstraints(maxLength = 200)
            ),
            FieldSchema(
                name = "category",
                type = FieldType.SELECT,
                constraints = FieldConstraints(enumValues = listOf("ICEBREAKER", "DEEP", "FUN"))
            )
        ),
        indexes = listOf("CREATE INDEX idx_prompts_category ON s_prompts (category)")
    )

    @Test
    fun postgresPlanContainsTableAndConstraints() {
        val plan = controller.planFor(DatabaseTarget.Postgres, listOf(promptsSchema))
        assertTrue(plan.statements.first().contains("CREATE TABLE IF NOT EXISTS \"s_prompts\""))
        assertTrue(plan.statements.first().contains("CHECK (\"category\" = ANY"))
        assertTrue(plan.statements.any { it.contains("CREATE INDEX idx_prompts_category") })
        assertTrue(plan.warnings.isEmpty())
    }

    @Test
    fun mongoPlanEmitsJsonSchemaValidator() {
        val plan = controller.planFor(DatabaseTarget.Mongo, listOf(promptsSchema))
        val createStatement = plan.statements.first()
        assertTrue(createStatement.contains("db.createCollection(\"s_prompts\""))
        assertTrue(createStatement.contains("required: [\"id\", \"text\"]"))
        assertTrue(createStatement.contains("enum: [\"ICEBREAKER\""))
    }

    @Test
    fun relationWithoutTargetRaisesWarning() {
        val schema = CollectionSchema(
            name = "s_user_values",
            fields = listOf(
                FieldSchema(
                    name = "userId",
                    type = FieldType.RELATION,
                    required = true
                ),
                FieldSchema(
                    name = "keyValueId",
                    type = FieldType.RELATION,
                    required = true,
                    constraints = FieldConstraints(relationCollection = "s_key_values")
                )
            )
        )

        val plan = controller.planFor(DatabaseTarget.Postgres, listOf(schema))
        assertTrue(plan.warnings.any { it.contains("userId") })
        assertEquals(0, plan.warnings.count { it.contains("keyValueId") })
        assertTrue(plan.statements.first().contains("\"s_user_values\""))
    }
}
