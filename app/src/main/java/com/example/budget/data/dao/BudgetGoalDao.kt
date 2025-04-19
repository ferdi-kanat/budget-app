package com.example.budget.data.dao

import androidx.room.*
import com.example.budget.data.entity.BudgetGoalEntity
import com.example.budget.data.BudgetProgress
import kotlinx.coroutines.flow.Flow

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
    fun getBudgetProgress(monthYear: String): Flow<List<BudgetProgress>>

    @Query("SELECT * FROM budget_goals ORDER BY createdAt DESC")
    fun getAllBudgetGoals(): Flow<List<BudgetGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetGoal(budgetGoal: BudgetGoalEntity)

    @Delete
    suspend fun deleteBudgetGoal(budgetGoal: BudgetGoalEntity)

    @Query("SELECT * FROM budget_goals WHERE monthYear = :monthYear")
    fun getBudgetGoalsByMonth(monthYear: String): Flow<List<BudgetGoalEntity>>

    @Query("SELECT * FROM budget_goals WHERE category = :category AND monthYear = :monthYear")
    fun getBudgetGoalByCategoryAndMonth(category: String, monthYear: String): Flow<BudgetGoalEntity?>

    @Query("SELECT * FROM budget_goals WHERE monthYear = :monthYear")
    suspend fun getBudgetGoalsForMonth(monthYear: String): List<BudgetGoalEntity>

    @Update
    suspend fun updateBudgetGoal(budgetGoal: BudgetGoalEntity)
}