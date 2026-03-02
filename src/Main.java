import java.sql.SQLException;

/**
 * University Course Timetabling System using Genetic Algorithm
 * Main entry point for the application
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("University Course Timetabling System");
        System.out.println("====================================");
        
        DatabaseManager dbManager = null;
        try {
            // Initialize database connection
            dbManager = new DatabaseManager();
            System.out.println("Database connection established.");
            
            // // Generate classes data
            // DataGenerator generator = new DataGenerator(dbManager);
            // generator.generateClasses();
            // generator.generateGroupClassAssignments();
            // System.out.println("Classes and group assignments generated.");
            
            for (int i = 1; i <= 10; i++) {
                // Initialize and run genetic algorithm
                GeneticAlgorithm ga = new GeneticAlgorithm(dbManager);
                Chromosome bestSolution = ga.run(i);
                
                // Generate output files
                OutputGenerator outputGen = new OutputGenerator(dbManager, bestSolution);
                outputGen.generateAllTimetables(i);
            }

            System.out.println("Timetabling process completed successfully!");

        } catch (Exception e) {
            System.err.println("Error during execution: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        
        } finally {
            // Clean up resources
            if (dbManager != null) {
                try {
                    dbManager.close();
                    System.out.println("Database connection closed.");
                    
                } catch (SQLException e) {
                    System.err.println("Error closing database connection: " + e.getMessage());
                }
            }
        }
    }
}
