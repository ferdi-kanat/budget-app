package com.example.budget.data.dao

import com.example.budget.data.entity.BudgetGoalEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BudgetGoalDao {
    @Query("""
    SELECT 
        bg.category as category,
        bg.targetAmount as targetAmount,
        COALESCE(SUM(
            CASE 
                WHEN t.amount < 0 AND t.category = bg.category
                THEN ABS(t.amount) 
                ELSE 0 
            END
        ), 0) as spentAmount
    FROM budget_goals bg
    LEFT JOIN transactions t ON 
        t.category = bg.category AND
        substr(t.date, 7, 4) || '-' || substr(t.date, 4, 2) = bg.monthYear
    WHERE bg.monthYear = :monthYear
    GROUP BY bg.category
    """)
    suspend fun getBudgetProgress(monthYear: String): List<BudgetProgress>

    @Query("SELECT * FROM budget_goals WHERE monthYear = :monthYear")
    suspend fun getBudgetGoalsForMonth(monthYear: String): List<BudgetGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetGoal(goal: BudgetGoalEntity)

    @Delete
    suspend fun deleteBudgetGoal(goal: BudgetGoalEntity)

    @Update
    suspend fun updateBudgetGoal(goal: BudgetGoalEntity)

    @Query("SELECT * FROM budget_goals WHERE id = :goalId")
    suspend fun getBudgetGoalById(goalId: Long): BudgetGoalEntity
}

data class BudgetProgress(
    val category: String,
    val targetAmount: Double,
    val spentAmount: Double
)