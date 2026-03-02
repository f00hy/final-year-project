import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates output files including room, student, course, and lecturer timetables
 */
public class OutputGenerator {
    private DatabaseManager dbManager;
    private Chromosome bestSolution;

    // Caches
    private Map<Integer, Room> roomCache;
    private Map<Integer, ClassDetail> classDetailCache;
    private Map<Integer, List<Integer>> classGroupsCache;
    private Map<Integer, Integer> classDurationCache;
    
    /**
     * Constructor
     */
    public OutputGenerator(DatabaseManager dbManager, Chromosome bestSolution) throws SQLException {
        this.dbManager = dbManager;
        this.bestSolution = bestSolution;
        initializeCaches();
    }

    /**
     * Initialize caches
     */
    private void initializeCaches() throws SQLException {
        roomCache = new HashMap<>();
        classDetailCache = new HashMap<>();
        classGroupsCache = new HashMap<>();
        classDurationCache = new HashMap<>();
        
        // Cache rooms
        List<Room> rooms = dbManager.getAllRooms();
        for (Room room : rooms) {
            roomCache.put(room.getId(), room);
        }
        
        // Cache classes and their details
        List<Class> classes = dbManager.getAllClasses();
        for (Class classObj : classes) {
            classDurationCache.put(classObj.getId(), classObj.getDuration());
            classDetailCache.put(classObj.getId(), dbManager.getClassDetail(classObj.getId()));
            classGroupsCache.put(classObj.getId(), dbManager.getGroupsByClass(classObj.getId()));
        }
    }
    
    /**
     * Generate all timetable files
     */
    public void generateAllTimetables(int exp) throws SQLException, IOException {
        System.out.println("Generating output files...");
        
        // Create output directory
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Create subdirectories
        File expDir = new File(outputDir, Integer.toString(exp));
        if (!expDir.exists()) {
            expDir.mkdirs();
        }

        String[] subDirs = {"rooms", "students", "courses", "lecturers"};
        for (String sub : subDirs) {
            File subDir = new File(expDir, sub);
            if (!subDir.exists()) {
                subDir.mkdirs();
            }
        }

        // Generate room timetables
        generateRoomTimetables(exp);
        
        // Generate student timetables
        generateStudentTimetables(exp);
        
        // Generate course timetables
        generateCourseTimetables(exp);
        
        // Generate lecturer timetables
        generateLecturerTimetables(exp);

        // Generate full timetable
        generateFullTimetable(exp);
        
        System.out.println("All timetable files generated successfully!");
    }
    
    /**
     * Generate room timetables
     */
    private void generateRoomTimetables(int exp) throws SQLException, IOException {
        for (Room room : roomCache.values()) {
            String filename = String.format("output/%d/rooms/room_%s.csv", exp, room.getName());
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                // Header
                writer.print("Day");
                for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                    writer.print("," + getTimeSlot(slot));
                }
                writer.println();
                
                // Each day as a row
                for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
                    writer.print(getDayName(day));
                    
                    for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                        int classId = bestSolution.getGeneAt(room.getId(), day, slot);
                        String cellContent = "----";
                        if (Chromosome.isPrayerSlot(day, slot)) {
                            cellContent = "Prayer";
                        }
                        
                        if (classId > 0) {
                            // Only process if this is the start of the class
                            if (!isSlotContinuation(room.getId(), day, slot, classId)) {
                                ClassDetail detail = classDetailCache.get(classId);

                                if (detail != null) {
                                    cellContent = String.format("%s (%s) - %s",
                                            detail.getCourseCode(),
                                            detail.getType().toUpperCase(),
                                            detail.getLecturerName());
                                    
                                    // Fill all slots for this class
                                    Integer duration = classDurationCache.get(classId);
                                    if (duration != null) {
                                        for (int d = 1; d < duration && slot + d <= Chromosome.NUM_SLOTS; d++) {
                                            cellContent += ",cont.";
                                        }
                                        slot += duration - 1; // Skip the remaining slots for this class
                                    }
                                }
                            }
                        }
                        writer.print("," + cellContent);
                    }
                    writer.println();
                }
            }
        }
        System.out.println("Room timetables generated in output/" + exp + "/rooms/ directory");
    }

    /**
     * Generate individual student timetables
     */
    private void generateStudentTimetables(int exp) throws SQLException, IOException {
        List<StudentGroup> groups = dbManager.getAllStudentGroups();
        
        for (StudentGroup group : groups) {
            List<Student> students = dbManager.getStudentsByGroup(group.getId());
            
            for (Student student : students) {
                generateStudentTimetable(exp, student, group.getId());
            }
        }
        
        System.out.println("Student timetables generated in output/" + exp + "/students/ directory");
    }
    
    /**
     * Generate timetable for a specific student
     */
    private void generateStudentTimetable(int exp, Student student, int groupId) throws SQLException, IOException {
        String filename = String.format("output/%d/students/student_%d.csv", exp, student.getId());

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Student: " + student.getName() + " (Group: " + groupId + ")");
            writer.println();

            // Header
            writer.print("Day");
            for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                writer.print("," + getTimeSlot(slot));
            }
            writer.println();

            // Each day as a row
            for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
                writer.print(getDayName(day));

                for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                    String cellContent = "----";
                    if (Chromosome.isPrayerSlot(day, slot)) {
                        cellContent = "Prayer";
                    }

                    // Scan rooms to check if this student has a class at (day, slot)
                    for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
                        int classId = bestSolution.getGeneAt(room, day, slot);

                        if (classId > 0) {
                            List<Integer> enrolledGroups = classGroupsCache.get(classId);

                            // Check if this student's group is enrolled in this class
                            if (enrolledGroups != null && enrolledGroups.contains(groupId)) {
                                // Only process if this is the start of the class
                                if (!isSlotContinuation(room, day, slot, classId)) {
                                    ClassDetail detail = classDetailCache.get(classId);
                                    Room roomObj = roomCache.get(room);

                                    if (detail != null && roomObj != null) {
                                        cellContent = String.format("%s (%s) - %s at %s",
                                                    detail.getCourseCode(),
                                                    detail.getType().toUpperCase(),
                                                    detail.getLecturerName(),
                                                    roomObj.getName());
                                        
                                        // Fill all slots for this class
                                        Integer duration = classDurationCache.get(classId);
                                        if (duration != null) {
                                            for (int d = 1; d < duration && slot + d <= Chromosome.NUM_SLOTS; d++) {
                                                cellContent += ",cont.";
                                            }
                                            slot += duration - 1; // Skip the remaining slots for this class
                                        }
                                    }
                                }
                                break; // Stop scanning other rooms once we found the student's class
                            }
                        }
                    }
                    writer.print("," + cellContent);
                }
                writer.println();
            }
        }
    }

    /**
     * Generate course timetables
     */
    private void generateCourseTimetables(int exp) throws SQLException, IOException {
        List<Course> courses = dbManager.getAllCourses();
        
        for (Course course : courses) {
            generateCourseTimetable(exp, course);
        }
        
        System.out.println("Course timetables generated in output/" + exp + "/courses/ directory");
    }
    
    /**
     * Generate timetable for a specific course
     */
    private void generateCourseTimetable(int exp, Course course) throws SQLException, IOException {
        String filename = String.format("output/%d/courses/course_%s.csv", exp, course.getCode());

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Course: " + course.getCode() + " - " + course.getName());
            writer.println();
            
            // Header
            writer.print("Day");
            for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                writer.print("," + getTimeSlot(slot));
            }
            writer.println();

            // Each day as a row
            for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
                writer.print(getDayName(day));

                // Remaining continuation slots
                Map<Integer, Integer> remaining = new HashMap<>();
    
                for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                    List<String> lines = new ArrayList<>();
    
                    // Scan rooms to check if this course has a class at (day, slot)
                    for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
                        Room roomObj = roomCache.get(room);

                        // Check if there are remaining continuation slots
                        Integer contLeft = remaining.get(room);
                        if (contLeft != null && contLeft > 0) {
                            lines.add("cont. at " + roomObj.getName());
                            remaining.put(room, contLeft - 1);
                            continue;
                        }

                        int classId = bestSolution.getGeneAt(room, day, slot);
    
                        if (classId > 0) {
                            ClassDetail detail = classDetailCache.get(classId);
    
                            // Check if this class belongs to the course
                            if (detail != null && detail.getCourseId() == course.getId()) {
                                // Only process if this is the start of the class
                                if (!isSlotContinuation(room, day, slot, classId)) {
                                    List<Integer> groups = classGroupsCache.get(classId);

                                    if (roomObj != null) {
                                        String groupsList = groups.stream().map(String::valueOf).collect(Collectors.joining(";"));
    
                                        String line = String.format("%s - %s at %s (Groups: %s)",
                                                detail.getType().toUpperCase(),
                                                detail.getLecturerName(),
                                                roomObj.getName(),
                                                groupsList);
                                        
                                        lines.add(line);

                                        // Add remaining continuation slots
                                        Integer duration = classDurationCache.get(classId);
                                        if (duration != null) {
                                            remaining.put(room, duration - 1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Fill the cell
                    if (lines.isEmpty()) {
                        if (Chromosome.isPrayerSlot(day, slot)) {
                            writer.print(",Prayer");
                        } else {
                            writer.print(",----");
                        }
                    } else {
                        writer.print(",\"" + String.join("\n", lines) + "\"");
                    }
                }
                writer.println();
            }
        }
    }
    
    /**
     * Generate lecturer timetables
     */
    private void generateLecturerTimetables(int exp) throws SQLException, IOException {
        List<Lecturer> lecturers = dbManager.getAllLecturers();
        
        for (Lecturer lecturer : lecturers) {
            generateLecturerTimetable(exp, lecturer);
        }
        
        System.out.println("Lecturer timetables generated in output/" + exp + "/lecturers/ directory");
    }
    
    /**
     * Generate timetable for a specific lecturer
     */
    private void generateLecturerTimetable(int exp, Lecturer lecturer) throws SQLException, IOException {
        String filename = String.format("output/%d/lecturers/lecturer_%d.csv", exp, lecturer.getId());
    
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Lecturer: " + lecturer.getName());
            writer.println();
    
            // Header
            writer.print("Day");
            for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                writer.print("," + getTimeSlot(slot));
            }
            writer.println();
    
            // Each day as a row
            for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
                writer.print(getDayName(day));
    
                for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                    String cellContent = "----";
                    if (Chromosome.isPrayerSlot(day, slot)) {
                        cellContent = "Prayer";
                    }
    
                    // Scan rooms to check if this lecturer teaches at (day, slot)
                    for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
                        int classId = bestSolution.getGeneAt(room, day, slot);
    
                        if (classId > 0) {
                            ClassDetail detail = classDetailCache.get(classId);
    
                            // Check if this class belongs to the lecturer
                            if (detail != null && detail.getLecturerId() == lecturer.getId()) {
                                // Only process if this is the start of the class
                                if (!isSlotContinuation(room, day, slot, classId)) {
                                    Room roomObj = roomCache.get(room);
                                    List<Integer> groups = classGroupsCache.get(classId);
    
                                    if (roomObj != null) {
                                        String groupsList = groups.stream().map(String::valueOf).collect(Collectors.joining(";"));
    
                                        cellContent = String.format("%s (%s) at %s (Groups: %s)",
                                                detail.getCourseCode(),
                                                detail.getType().toUpperCase(),
                                                roomObj.getName(),
                                                groupsList);
    
                                        // Fill all slots for this class
                                        Integer duration = classDurationCache.get(classId);
                                        if (duration != null) {
                                            for (int d = 1; d < duration && slot + d <= Chromosome.NUM_SLOTS; d++) {
                                                cellContent += ",cont.";
                                            }
                                            slot += duration - 1; // Skip the remaining slots for this class
                                        }
                                    }
                                }
                                break; // Stop scanning rooms once lecturer's class is found
                            }
                        }
                    }
                    writer.print("," + cellContent);
                }
                writer.println();
            }
        }
    }

    /**
     * Generate a full timetable that contains all information
     */
    private void generateFullTimetable(int exp) throws SQLException, IOException {
        String filename = String.format("output/%d/timetable.csv", exp);

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Header
            writer.print("Day");
            for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                writer.print("," + getTimeSlot(slot));
            }
            writer.println();

            // Each day as a row
            for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
                writer.print(getDayName(day));

                // Remaining continuation slots
                Map<Integer, Integer> remaining = new HashMap<>();
    
                for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                    List<String> lines = new ArrayList<>();
    
                    // Scan rooms to check if this slot has a class
                    for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
                        Room roomObj = roomCache.get(room);

                        // Check if there are remaining continuation slots
                        Integer contLeft = remaining.get(room);
                        if (contLeft != null && contLeft > 0) {
                            lines.add("cont. at " + roomObj.getName());
                            remaining.put(room, contLeft - 1);
                            continue;
                        }

                        int classId = bestSolution.getGeneAt(room, day, slot);
    
                        if (classId > 0) {
                            ClassDetail detail = classDetailCache.get(classId);
    
                            if (detail != null) {
                                // Only process if this is the start of the class
                                if (!isSlotContinuation(room, day, slot, classId)) {
                                    List<Integer> groups = classGroupsCache.get(classId);

                                    if (roomObj != null) {
                                        String groupsList = groups.stream().map(String::valueOf).collect(Collectors.joining(";"));
    
                                        String line = String.format("%s (%s) - %s at %s (Groups: %s)",
                                                detail.getCourseCode(),
                                                detail.getType().toUpperCase(),
                                                detail.getLecturerName(),
                                                roomObj.getName(),
                                                groupsList);
                                        
                                        lines.add(line);

                                        // Add remaining continuation slots
                                        Integer duration = classDurationCache.get(classId);
                                        if (duration != null) {
                                            remaining.put(room, duration - 1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Fill the cell
                    if (lines.isEmpty()) {
                        if (Chromosome.isPrayerSlot(day, slot)) {
                            writer.print(",Prayer");
                        } else {
                            writer.print(",----");
                        }
                    } else {
                        writer.print(",\"" + String.join("\n", lines) + "\"");
                    }
                }
                writer.println();
            }
        }
        System.out.println("Full timetable saved to output/" + exp + "/timetable.csv");
    }
    
    /**
     * Get the day name for a given day
     */
    private String getDayName(int day) {
        switch (day) {
            case 1: return "Monday";
            case 2: return "Tuesday";
            case 3: return "Wednesday";
            case 4: return "Thursday";
            case 5: return "Friday";
            default: return "Unknown";
        }
    }
    
    /**
     * Get the time slot for a given slot
     */
    private String getTimeSlot(int slot) {
        int hour = 8 + (slot - 1);
        return String.format("%02d:00-%02d:00", hour, hour + 1);
    }

    /**
     * Check if a slot is a continuation of a class
     */
    private boolean isSlotContinuation(int room, int day, int slot, int classId) {
        if (slot > 1) {
            int prevClassId = bestSolution.getGeneAt(room, day, slot - 1);
            return prevClassId == classId;
        }
        return false;
    }
}
