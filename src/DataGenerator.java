import java.sql.SQLException;
import java.util.*;

/**
 * Generates classes and group assignments based on course data
 */
public class DataGenerator {
    // Define class types and durations for each course based on data.txt
    private static final Map<String, String> COURSE_CLASSES = Map.of(
        "UCCD1024", "lecture:3,practical:2",
        "UCCD1203", "lecture:2,practical:2",
        "UCCD2003", "lecture:3,tutorial:1",
        "UCCM1353", "lecture:3,tutorial:1",
        "UCCM1363", "lecture:3,tutorial:1",
        "MPU3152", "lecture:2"
    );

    private DatabaseManager dbManager;
    
    /*
     * Constructor
     */
    public DataGenerator(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Generate classes for all courses
     */
    public void generateClasses() throws SQLException {
        System.out.println("Generating classes...");
        
        List<Course> courses = dbManager.getAllCourses();
        
        for (Course course : courses) {
            String classDefinition = COURSE_CLASSES.get(course.getCode());
            if (classDefinition != null) {
                generateClassesForCourse(course, classDefinition);
            }
        }
        
        System.out.println("Classes generation completed.");
    }
    
    /**
     * Generate classes for a specific course
     */
    private void generateClassesForCourse(Course course, String classDefinition) throws SQLException {
        String[] classTypes = classDefinition.split(",");
        
        for (String classTypeInfo : classTypes) {
            String[] parts = classTypeInfo.split(":");
            String type = parts[0];
            int duration = Integer.parseInt(parts[1]);
            
            // Calculate number of classes needed based on student count and room capacity
            int numClassesNeeded = calculateNumClassesNeeded(type, 150);
            
            for (int i = 0; i < numClassesNeeded; i++) {
                // Assign lecturer with minimum hours for this course
                Lecturer assignedLecturer = getLecturerWithMinHours(course.getId());
                
                // Create class
                Class newClass = new Class(course.getId(), assignedLecturer.getId(), type, duration);
                int classId = dbManager.insertClass(newClass);
                
                // Update lecturer's assigned hours
                dbManager.updateLecturerAssignedHours(assignedLecturer.getId(), duration);
                
                System.out.printf("Created %s class (ID: %d) for course %s, duration: %d hours, lecturer: %s%n",
                        type, classId, course.getCode(), duration, assignedLecturer.getName());
            }
        }
    }
    
    /**
     * Calculate number of classes needed based on type and total students
     */
    private int calculateNumClassesNeeded(String type, int totalStudents) {
        int maxCapacity;
        switch (type.trim().toLowerCase()) {
            case "lecture":
                maxCapacity = 300;
                break;
            case "tutorial":
                maxCapacity = 30;
                break;
            case "practical":
                maxCapacity = 20;
                break;
            default:
                maxCapacity = 1;
        }
        
        return (int) Math.ceil((double) totalStudents / maxCapacity);
    }
    
    /**
     * Get lecturer with minimum assigned hours for a course
     */
    private Lecturer getLecturerWithMinHours(int courseId) throws SQLException {
        List<Lecturer> lecturers = dbManager.getLecturersByCourse(courseId);
        if (lecturers.isEmpty()) {
            throw new SQLException("No lecturers found for course ID: " + courseId);
        }
        
        // Return the first lecturer (ordered by assigned class hours in ascending order)
        return lecturers.get(0);
    }
    
    /**
     * Generate group-class assignments for all groups and classes
     */
    public void generateGroupClassAssignments() throws SQLException {
        System.out.println("Generating group-class assignments...");
        
        List<StudentGroup> groups = dbManager.getAllStudentGroups();
        List<Class> classes = dbManager.getAllClasses();
        List<Course> courses = dbManager.getAllCourses();

        // Map courses by ID
        Map<Integer, Course> coursesById = new HashMap<>();
        for (Course c : courses) {
            coursesById.put(c.getId(), c);
        }
        
        // Group classes by course and type
        Map<String, List<Class>> classesByCourseAndType = new HashMap<>();
        for (Class classObj : classes) {
            Course course = coursesById.get(classObj.getCourseId());
            if (course == null) continue;
            String key = course.getCode() + "_" + classObj.getType();
            classesByCourseAndType.computeIfAbsent(key, k -> new ArrayList<>()).add(classObj);
        }
        
        // Get group counts for all classes
        Map<Integer, Integer> classGroupCounts = new HashMap<>();
        for (Class classObj : classes) {
            classGroupCounts.put(classObj.getId(), 0);
        }

        // For each group, assign to classes for each course
        for (StudentGroup group : groups) {
            assignGroupToClasses(group, courses, classesByCourseAndType, classGroupCounts);
        }
        
        System.out.println("Group-class assignments completed.");
    }
    
    /**
     * Assign a student group to appropriate classes for all courses
     */
    private void assignGroupToClasses(StudentGroup group, List<Course> courses, Map<String, List<Class>> classesByCourseAndType, Map<Integer, Integer> classGroupCounts) throws SQLException {
        for (Course course : courses) {
            String classDefinition = COURSE_CLASSES.get(course.getCode());
            if (classDefinition == null) continue;

            String[] classTypes = classDefinition.split(",");
            for (String classTypeInfo : classTypes) {
                String type = classTypeInfo.split(":")[0];
                String key = course.getCode() + "_" + type;
                
                List<Class> availableClasses = classesByCourseAndType.get(key);
                if (availableClasses == null || availableClasses.isEmpty()) continue;
                
                // Find the class with least enrolled groups
                Class assignedClass = findLeastLoadedClass(availableClasses, classGroupCounts);
                
                try {
                    dbManager.insertGroupClassAssignment(group.getId(), assignedClass.getId());
                    classGroupCounts.put(assignedClass.getId(), classGroupCounts.get(assignedClass.getId()) + 1);
                    System.out.printf("Assigned group %d to class %d (%s %s)%n",
                            group.getId(), assignedClass.getId(), course.getCode(), type);
                } catch (SQLException e) {
                    // Handle capacity constraint violation
                    System.err.printf("Failed to assign group %d to class %d: %s%n",
                            group.getId(), assignedClass.getId(), e.getMessage());
                }
            }
        }
    }
    
    /**
     * Find the class with the least number of enrolled groups
     */
    private Class findLeastLoadedClass(List<Class> classes, Map<Integer, Integer> classGroupCounts) {
        Class leastLoadedClass = classes.get(0);
        int minGroupCount = classGroupCounts.get(leastLoadedClass.getId());
        
        for (Class classObj : classes) {
            int groupCount = classGroupCounts.get(classObj.getId());
            if (groupCount < minGroupCount) {
                minGroupCount = groupCount;
                leastLoadedClass = classObj;
            }
        }
        
        return leastLoadedClass;
    }
}
