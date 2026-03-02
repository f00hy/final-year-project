import java.sql.SQLException;
import java.util.*;

/**
 * Evaluates fitness of chromosomes based on constraint violations
 * Uses a penalty-cost function where lower values indicate better fitness
 */
public class FitnessEvaluator {
    private static final int HARD_CONSTRAINT_PENALTY = 10000;
    private static final int SOFT_CONSTRAINT_PENALTY = 10;
    private static final int CONSECUTIVE_DIFFERENT_BUILDING_PENALTY = 20;

    private List<Class> allClasses;
    private List<Room> allRooms;
    private List<Lecturer> allLecturers;
    private Map<Integer, List<Integer>> classToGroups;
    private Map<Integer, ClassDetail> classDetails;
    private Map<Integer, Room> roomCache;
    private Map<Integer, Class> classCache;
    private Map<Integer, Integer> classDurations;
    
    public FitnessEvaluator(DatabaseManager dbManager) throws SQLException {
        this.allClasses = dbManager.getAllClasses();
        this.allRooms = dbManager.getAllRooms();
        this.allLecturers = dbManager.getAllLecturers();
        this.classToGroups = new HashMap<>();
        this.classDetails = new HashMap<>();
        this.roomCache = new HashMap<>();
        this.classCache = new HashMap<>();
        this.classDurations = new HashMap<>();
        
        // Initialize room cache
        for (Room room : allRooms) {
            roomCache.put(room.getId(), room);
        }
        
        // Initialize class-to-groups mapping, class details, class cache, and class durations
        for (Class classObj : allClasses) {
            classToGroups.put(classObj.getId(), dbManager.getGroupsByClass(classObj.getId()));
            classDetails.put(classObj.getId(), dbManager.getClassDetail(classObj.getId()));
            classCache.put(classObj.getId(), classObj);
            classDurations.put(classObj.getId(), classObj.getDuration());
        }
    }
    
    /**
     * Calculate fitness for a chromosome
     */
    public int calculateFitness(Chromosome chromosome) {
        int totalPenalty = 0;

        // Compute class occurrences for efficient access
        Map<Integer, Set<Integer>> classOccurrences = computeClassOccurrences(chromosome);
        
        // Hard constraints
        totalPenalty += checkStudentTimeConflicts(chromosome);
        totalPenalty += checkLecturerTimeConflicts(chromosome);
        totalPenalty += checkRoomTypeCompatibility(chromosome, classOccurrences);
        totalPenalty += checkRoomCapacity(chromosome, classOccurrences);
        totalPenalty += checkFridayPrayerTime(chromosome);
        totalPenalty += checkMissingAndDuplicateClasses(chromosome, classOccurrences);
        totalPenalty += checkCrossDayBoundary(chromosome);
        
        // Soft constraints
        totalPenalty += checkConsecutiveHoursStudents(chromosome);
        // totalPenalty += checkConsecutiveHoursLecturers(chromosome);
        totalPenalty += checkConsecutiveClassesBuilding(chromosome);

        return totalPenalty;
    }
    
    /**
     * Hard Constraint: A student must attend at most one class per time slot
     */
    private int checkStudentTimeConflicts(Chromosome chromosome) {
        int penalty = 0;
        
        for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
            for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                Map<Integer, Integer> groupClassCounts = new HashMap<>();
                
                // Check all rooms for this time slot
                for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
                    int classId = chromosome.getGeneAt(room, day, slot);
                    if (classId > 0) {
                        List<Integer> groups = classToGroups.get(classId);
                        if (groups != null) {
                            for (int groupId : groups) {
                                int count = groupClassCounts.merge(groupId, 1, Integer::sum);
                                if (count > 1) {
                                    penalty += HARD_CONSTRAINT_PENALTY;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return penalty;
    }
    
    /**
     * Hard Constraint: A lecturer must teach at most one class per time slot
     */
    private int checkLecturerTimeConflicts(Chromosome chromosome) {
        int penalty = 0;
        
        for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
            for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                Map<Integer, Integer> lecturerClassCounts = new HashMap<>();
                
                // Check all rooms for this time slot
                for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
                    int classId = chromosome.getGeneAt(room, day, slot);
                    if (classId > 0) {
                        ClassDetail detail = classDetails.get(classId);
                        if (detail != null) {
                            int count = lecturerClassCounts.merge(detail.getLecturerId(), 1, Integer::sum);
                            if (count > 1) {
                                penalty += HARD_CONSTRAINT_PENALTY;
                            }
                        }
                    }
                }
            }
        }
        
        return penalty;
    }
    
    /**
     * Hard Constraint: Class type and room type compatibility
     */
    private int checkRoomTypeCompatibility(Chromosome chromosome, Map<Integer, Set<Integer>> classOccurrences) {
        int penalty = 0;
        
        for (Map.Entry<Integer, Set<Integer>> entry : classOccurrences.entrySet()) {
            int classId = entry.getKey();
            ClassDetail detail = classDetails.get(classId);
            if (detail == null) continue;
            
            // Check the first occurrence of each class
            Integer firstOccurrence = entry.getValue().iterator().next();
            Chromosome.SlotCoordinates coords = Chromosome.getCoordinates(firstOccurrence);
            Room roomObj = roomCache.get(coords.roomId);
            
            if (roomObj != null && !roomObj.isCompatibleWith(detail.getType())) {
                penalty += HARD_CONSTRAINT_PENALTY;
            }
        }
        
        return penalty;
    }
    
    /**
     * Hard Constraint: Room capacity check
     */
    private int checkRoomCapacity(Chromosome chromosome, Map<Integer, Set<Integer>> classOccurrences) {
        int penalty = 0;
        
        for (Map.Entry<Integer, Set<Integer>> entry : classOccurrences.entrySet()) {
            int classId = entry.getKey();
            List<Integer> groups = classToGroups.get(classId);
            if (groups == null) continue;
            
            // Check the first occurrence of each class
            Integer firstOccurrence = entry.getValue().iterator().next();
            Chromosome.SlotCoordinates coords = Chromosome.getCoordinates(firstOccurrence);
            Room roomObj = roomCache.get(coords.roomId);
            
            if (roomObj != null) {
                int totalStudents = groups.size() * 10; // Each group has 10 students
                if (totalStudents > roomObj.getCapacity()) {
                    penalty += HARD_CONSTRAINT_PENALTY;
                }
            }
        }
        
        return penalty;
    }

    /**
     * Hard Constraint: Classes must be scheduled on weekdays only (already satisfied by design)
     */
    
    /**
     * Hard Constraint: Classes must be scheduled between 08:00 and 18:00 (already satisfied by design)
     */
    
    /**
     * Hard Constraint: No classes on Friday between 12:00 and 14:00 (slots 5-6)
     */
    private int checkFridayPrayerTime(Chromosome chromosome) {
        int penalty = 0;
        
        for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
            // 12:00-14:00 on Friday
            for (int slot = 5; slot <= 6; slot++) {
                int classId = chromosome.getGeneAt(room, 5, slot);
                if (classId > 0) {
                    penalty += HARD_CONSTRAINT_PENALTY;
                }
            }
        }
        
        return penalty;
    }
    
    /**
     * Hard Constraint: All classes must be scheduled once and only once
     */
    private int checkMissingAndDuplicateClasses(Chromosome chromosome, Map<Integer, Set<Integer>> classOccurrences) {
        int penalty = 0 ;
        
        for (Class classObj : allClasses) {
            Set<Integer> occurrences = classOccurrences.get(classObj.getId());
            if (occurrences == null || occurrences.size() != classObj.getDuration()) {
                penalty += HARD_CONSTRAINT_PENALTY;
            }
        }
        
        return penalty;
    }

    /**
     * Hard Constraint: A class must not span across two days
     */
    private int checkCrossDayBoundary(Chromosome chromosome) {
        int penalty = 0;
        
        for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
            for (int day = 1; day <= Chromosome.NUM_DAYS - 1; day++) {
                int endGene = chromosome.getGeneAt(room, day, Chromosome.NUM_SLOTS);
                if (endGene == 0) continue;
                int startNext = chromosome.getGeneAt(room, day + 1, 1);
                if (startNext == endGene) {
                    penalty += HARD_CONSTRAINT_PENALTY;
                }
            }
        }
        
        return penalty;
    }
    
    /**
     * Soft Constraint: Students should study no more than four consecutive hours
     */
    private int checkConsecutiveHoursStudents(Chromosome chromosome) {
        int penalty = 0;
        
        // Check each student group
        for (int groupId = 1; groupId <= 15; groupId++) {
            for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
                int consecutiveHours = 0;
                int maxConsecutive = 0;
                
                for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                    boolean hasClass = false;
                    
                    // Check if this group has a class in any room at this time
                    for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
                        int classId = chromosome.getGeneAt(room, day, slot);
                        if (classId > 0) {
                            List<Integer> groups = classToGroups.get(classId);
                            if (groups != null && groups.contains(groupId)) {
                                hasClass = true;
                                break;
                            }
                        }
                    }
                    
                    if (hasClass) {
                        consecutiveHours++;
                        maxConsecutive = Math.max(maxConsecutive, consecutiveHours);
                    } else {
                        consecutiveHours = 0;
                    }
                }
                
                if (maxConsecutive > 4) {
                    penalty += SOFT_CONSTRAINT_PENALTY * (maxConsecutive - 4);
                }
            }
        }
        
        return penalty;
    }

    // /**
    //  * Soft Constraint: Lecturers should teach no more than four consecutive hours
    //  */
    // private int checkConsecutiveHoursLecturers(Chromosome chromosome) {
    //     int penalty = 0;
        
    //     // Check each lecturer
    //     for (Lecturer lecturer : allLecturers) {
    //         for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
    //             int consecutiveHours = 0;
    //             int maxConsecutive = 0;
                
    //             for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
    //                 boolean hasClass = false;
                    
    //                 // Check if this lecturer has a class in any room at this time
    //                 for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
    //                     int classId = chromosome.getGeneAt(room, day, slot);
    //                     if (classId > 0) {
    //                         ClassDetail detail = classDetails.get(classId);
    //                         if (detail != null && detail.getLecturerId() == lecturer.getId()) {
    //                             hasClass = true;
    //                             break;
    //                         }
    //                     }
    //                 }
                    
    //                 if (hasClass) {
    //                     consecutiveHours++;
    //                     maxConsecutive = Math.max(maxConsecutive, consecutiveHours);
    //                 } else {
    //                     consecutiveHours = 0;
    //                 }
    //             }
                
    //             if (maxConsecutive > 4) {
    //                 penalty += SOFT_CONSTRAINT_PENALTY * (maxConsecutive - 4);
    //             }
    //         }
    //     }
        
    //     return penalty;
    // }
    
    /**
     * Soft Constraint: Student's consecutive classes should be held in the same building
     */
    private int checkConsecutiveClassesBuilding(Chromosome chromosome) {
        int penalty = 0;
        
        // Check each student group
        for (int groupId = 1; groupId <= 15; groupId++) {
            for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
                char previousBuilding = 0;
                boolean inConsecutiveSequence = false;
                
                for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                    char currentBuilding = 0;
                    boolean hasClass = false;
                    
                    // Find which room (building) this group has class in
                    for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
                        int classId = chromosome.getGeneAt(room, day, slot);
                        if (classId > 0) {
                            List<Integer> groups = classToGroups.get(classId);
                            if (groups != null && groups.contains(groupId)) {
                                Room roomObj = roomCache.get(room);
                                if (roomObj != null) {
                                    currentBuilding = roomObj.getBuilding();
                                    hasClass = true;
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (hasClass) {
                        if (inConsecutiveSequence && currentBuilding != previousBuilding) {
                            penalty += CONSECUTIVE_DIFFERENT_BUILDING_PENALTY;
                        }
                        previousBuilding = currentBuilding;
                        inConsecutiveSequence = true;
                    } else {
                        previousBuilding = 0;
                        inConsecutiveSequence = false;
                    }
                }
            }
        }
        
        return penalty;
    }

    /**
     * Soft Constraint: Lecturers should receive at least one teaching hour (already satisfied after data generation)
     */

    /**
     * Compute class occurrences
     */
    private Map<Integer, Set<Integer>> computeClassOccurrences(Chromosome chromosome) {
        Map<Integer, Set<Integer>> classOccurrences = new HashMap<>();
        
        for (int i = 0; i < Chromosome.TOTAL_GENES; i++) {
            int classId = chromosome.getGene(i);
            if (classId > 0) {
                classOccurrences.computeIfAbsent(classId, k -> new HashSet<>()).add(i);
            }
        }
        
        return classOccurrences;
    }
}
