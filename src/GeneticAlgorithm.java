import java.io.*;
import java.sql.SQLException;
import java.util.*;

/**
 * Main Genetic Algorithm implementation for university course timetabling
 */
public class GeneticAlgorithm {
    // GA Parameters
    private static final int POPULATION_SIZE = 100;
    private static final double CROSSOVER_RATE = 0.7;
    private static final double MUTATION_RATE = 0.4;
    
    private FitnessEvaluator fitnessEvaluator;
    private List<Class> allClasses;
    private List<Room> allRooms;
    private Map<Integer, Room> roomCache;
    private Map<Integer, Class> classCache;
    private Map<String, List<Integer>> roomsByClassType;
    private Map<String, List<Integer>> roomsByRoomType;
    private Random random;
    private int numGenerations;
    
    // Statistics
    private List<Integer> bestFitnessPerGeneration;
    private long startTime;
    
    public GeneticAlgorithm(DatabaseManager dbManager) throws SQLException {
        this.fitnessEvaluator = new FitnessEvaluator(dbManager);
        this.allClasses = dbManager.getAllClasses();
        this.allRooms = dbManager.getAllRooms();
        this.random = new Random();
        this.bestFitnessPerGeneration = new ArrayList<>();
        this.numGenerations = 0;

        initializeCaches();
    }
    
    /**
     * Initialize caches
     */
    private void initializeCaches() {
        roomCache = new HashMap<>();
        classCache = new HashMap<>();
        roomsByClassType = new HashMap<>();
        roomsByRoomType = new HashMap<>();

        for (Room room : allRooms) {
            roomCache.put(room.getId(), room);
        }

        for (Class classObj : allClasses) {
            classCache.put(classObj.getId(), classObj);
        }
       
        for (String classType : new String[] {"lecture", "tutorial", "practical"}) {
            roomsByClassType.put(classType, new ArrayList<>());
            for (Room room : allRooms) {
                if (room.isCompatibleWith(classType)) {
                    roomsByClassType.get(classType).add(room.getId());
                }
            }
        }

        for (String roomType : new String[] {"lecture hall", "tutorial room", "practical lab"}) {
            roomsByRoomType.put(roomType, new ArrayList<>());
            for (Room room : allRooms) {
                if (room.getType().equals(roomType)) {
                    roomsByRoomType.get(roomType).add(room.getId());
                }
            }
        }
    }
    
    /**
     * Run the genetic algorithm
     */
    public Chromosome run(int exp) throws SQLException, IOException {
        System.out.println("Starting Genetic Algorithm...");
        System.out.printf("Population Size: %d, Crossover Rate: %.2f, Mutation Rate: %.2f\n",
                POPULATION_SIZE, CROSSOVER_RATE, MUTATION_RATE);
        
        startTime = System.nanoTime();
        
        // Initialize population
        List<Chromosome> population = initialisePopulation();
        System.out.println("Initial population created and evaluated.");
        
        Chromosome bestChromosome = getBestChromosome(population);
        System.out.printf("Initial best fitness: %d\n", bestChromosome.getFitness());
        
        // Evolution loop
        while (true) {
            // Selection
            Chromosome[] parents = binaryTournamentSelection(population);
            Chromosome parent1 = parents[0];
            Chromosome parent2 = parents[1];

            Chromosome offspring1 = new Chromosome(parent1);
            Chromosome offspring2 = new Chromosome(parent2);

            // Crossover
            if (random.nextDouble() < CROSSOVER_RATE) {
                Chromosome[] offsprings = uniformCrossover(parent1, parent2);
                offspring1 = offsprings[0];
                offspring2 = offsprings[1];
                
                // Repair offspring
                repairChromosome(offspring1);
                repairChromosome(offspring2);
            }

            // Mutation
            if (random.nextDouble() < MUTATION_RATE) {
                swapMutation(offspring1);
                repairChromosome(offspring1);
            }
            if (random.nextDouble() < MUTATION_RATE) {
                swapMutation(offspring2);
                repairChromosome(offspring2);
            }

            // Evaluate fitness of offspring
            offspring1.setFitness(fitnessEvaluator.calculateFitness(offspring1));
            offspring2.setFitness(fitnessEvaluator.calculateFitness(offspring2));
            
            // Replacement
            weakChromosomeReplacement(population, parent1, parent2, offspring1, offspring2);

            // Update best chromosome
            bestChromosome = getBestChromosome(population);
            bestFitnessPerGeneration.add(bestChromosome.getFitness());

            // Update generation count
            numGenerations++;

            // Progress reporting
            if (numGenerations == 1 || numGenerations % 100 == 0) {
                System.out.printf("Generation %d: Best Fitness = %d\n", numGenerations, bestChromosome.getFitness());
            }
            
            // Termination criteria
            if (bestChromosome.getFitness() == 0) {
                System.out.printf("Optimal solution found at generation %d\n", numGenerations);
                break;
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        System.out.printf("Genetic Algorithm completed in %.2f seconds\n", (double) totalTime / 1000000000.0);
        System.out.printf("Final best fitness: %d\n", bestChromosome.getFitness());
        
        // Save statistics
        saveStatistics(exp, totalTime, bestChromosome);
        
        return bestChromosome;
    }
    
    /**
     * Initialize random population
     */
    private List<Chromosome> initialisePopulation() {
        List<Chromosome> population = new ArrayList<>();
        
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Chromosome chromosome = createRandomChromosome();
            repairChromosome(chromosome);
            chromosome.setFitness(fitnessEvaluator.calculateFitness(chromosome));
            population.add(chromosome);
        }
        
        return population;
    }
    
    /**
     * Create a random chromosome
     */
    private Chromosome createRandomChromosome() {
        Chromosome chromosome = new Chromosome();

        // Get classes to place
        List<Class> classesToPlace = new ArrayList<>(allClasses);

        // Sort classes by duration in descending order
        classesToPlace.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));

        for (Class classObj : classesToPlace) {
            placeClassRandomly(chromosome, classObj);
        }
        
        return chromosome;
    }

    /**
     * Place a class randomly in compatible rooms with sufficient space
     */
    private boolean placeClassRandomly(Chromosome chromosome, Class classObj) {
        List<Integer> compatibleRooms = roomsByClassType.get(classObj.getType());
        if (compatibleRooms.isEmpty()) return false;
        
        Collections.shuffle(compatibleRooms, random);
        
        for (int roomId : compatibleRooms) {
            if (placeClassInRoom(chromosome, classObj, roomId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Place a class in a specific room
     */
    private boolean placeClassInRoom(Chromosome chromosome, Class classObj, int roomId) {
        List<Integer> availableSlots = new ArrayList<>();
        
        // Find all consecutive available slots in this room
        for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
            for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                if (hasConsecutiveSlots(chromosome, roomId, day, slot, classObj.getDuration())) {
                    availableSlots.add(Chromosome.getSlotIndex(roomId, day, slot));
                }
            }
        }
        
        if (!availableSlots.isEmpty()) {
            int chosenSlot = availableSlots.get(random.nextInt(availableSlots.size()));
            setBlockOfGenes(chromosome, classObj.getId(), chosenSlot, classObj.getDuration());
            return true;
        }
        
        return false;
    }

    /**
     * Check if there are enough consecutive empty slots
     */
    private boolean hasConsecutiveSlots(Chromosome chromosome, int roomId, int day, int startSlot, int duration) {
        if (startSlot + duration - 1 > Chromosome.NUM_SLOTS) return false;
        
        for (int i = 0; i < duration; i++) {
            if (Chromosome.isPrayerSlot(day, startSlot + i) || // Check Friday prayer time
                (chromosome.getGeneAt(roomId, day, startSlot + i) != 0)) {
                return false;
            }
        }

        return true;
    }

    /**
     * ======================
     * Selection
     * ======================
     */

    private Chromosome[] rouletteWheelSelection(List<Chromosome> population) {
        Chromosome[] parents = new Chromosome[2];

        // Find the worst fitness
        int worstFitness = Collections.max(population, Comparator.comparingInt(Chromosome::getFitness)).getFitness();
        
        // Convert fitness to selection probability
        // Better fitness (lower penalty) gets higher probability
        int totalFitness = 0;
        for (Chromosome chromosome : population) {
            totalFitness += (worstFitness - chromosome.getFitness() + 1);
        }
        
        // Select two parents
        for (int i = 0; i < 2; i++) {
            int randomValue = random.nextInt(totalFitness);
            int cumulativeFitness = 0;

            for (Chromosome chromosome : population) {
                cumulativeFitness += (worstFitness - chromosome.getFitness() + 1);
                if (cumulativeFitness > randomValue) {
                    parents[i] = chromosome;
                    break;
                }
            }
        }

        return parents;
    }

    private Chromosome[] randomSelection(List<Chromosome> population) {
        Chromosome[] parents = new Chromosome[2];
        
        // Select two parents
        for (int i = 0; i < 2; i++) {
            parents[i] = population.get(random.nextInt(population.size()));
        }

        return parents;
    }

    private Chromosome[] binaryTournamentSelection(List<Chromosome> population) {
        Chromosome[] parents = new Chromosome[2];
        
        // Select two parents
       for (int i = 0; i < 2; i++) {
            // Randomly select two candidates
            int candidate1 = random.nextInt(population.size());
            int candidate2 = random.nextInt(population.size());
            while (candidate1 == candidate2) {
                candidate2 = random.nextInt(population.size());
            }
            
            // Get fitness to compare
            int fitness1 = population.get(candidate1).getFitness();
            int fitness2 = population.get(candidate2).getFitness();

            // Select the better candidate as parent
            parents[i] = (fitness1 <= fitness2) ? population.get(candidate1) : population.get(candidate2);
       }

        return parents;
    }

    private Chromosome[] linearRankingSelection(List<Chromosome> population) {
        Chromosome[] parents = new Chromosome[2];

        // Create a copy and sort by fitness (ascending - better fitness first)
        List<Chromosome> sorted = new ArrayList<>(population);
        sorted.sort(Comparator.comparingInt(Chromosome::getFitness));

        // Calculate total selection weight
        // Linear ranking: rank 1 gets highest weight, rank n gets lowest weight
        int totalWeight = 0;
        for (int i = 0; i < sorted.size(); i++) {
            totalWeight += (sorted.size() - i);
        }

        // Select two parents
        for (int i = 0; i < 2; i++) {
            int randomValue = random.nextInt(totalWeight);
            int cumulativeWeight = 0;

            for (int j = 0; j < sorted.size(); j++) {
                cumulativeWeight += (sorted.size() - j);
                if (cumulativeWeight > randomValue) {
                    parents[i] = sorted.get(j);
                    break;
                }
            }
        }

        return parents;
    }

    /**
     * ======================
     * Crossover
     * ======================
     */

    private Chromosome[] singlePointCrossover(Chromosome parent1, Chromosome parent2) {
        Chromosome[] offsprings = { new Chromosome(), new Chromosome() };
        
        // Randomly select crossover point (room)
        // Avoid selecting the first room
        int crossoverRoom = random.nextInt(Chromosome.NUM_ROOMS - 1) + 2;
        
        for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
            if (room < crossoverRoom) {
                // Copy from parent1 to offspring1, parent2 to offspring2
                offsprings[0].setRoomSlots(room, parent1.getRoomSlots(room));
                offsprings[1].setRoomSlots(room, parent2.getRoomSlots(room));
            } else {
                // Copy from parent2 to offspring1, parent1 to offspring2
                offsprings[0].setRoomSlots(room, parent2.getRoomSlots(room));
                offsprings[1].setRoomSlots(room, parent1.getRoomSlots(room));
            }
        }
        
        return offsprings;
    }

    private Chromosome[] twoPointCrossover(Chromosome parent1, Chromosome parent2) {
        Chromosome[] offsprings = { new Chromosome(), new Chromosome() };
        
        // Randomly select crossover points (rooms)
        // Avoid selecting the first room
        int crossoverRoom1 = random.nextInt(Chromosome.NUM_ROOMS - 1) + 2;
        int crossoverRoom2 = random.nextInt(Chromosome.NUM_ROOMS - 1) + 2;
        while (crossoverRoom1 == crossoverRoom2) {
            crossoverRoom2 = random.nextInt(Chromosome.NUM_ROOMS - 1) + 2;
        }

        // Ensure crossoverRoom1 is less than crossoverRoom2
        if (crossoverRoom1 > crossoverRoom2) {
            int temp = crossoverRoom1;
            crossoverRoom1 = crossoverRoom2;
            crossoverRoom2 = temp;
        }
        
        for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
            if (room < crossoverRoom1) {
                // Copy from parent1 to offspring1, parent2 to offspring2
                offsprings[0].setRoomSlots(room, parent1.getRoomSlots(room));
                offsprings[1].setRoomSlots(room, parent2.getRoomSlots(room));
            } else if (room < crossoverRoom2) {
                // Copy from parent2 to offspring1, parent1 to offspring2
                offsprings[0].setRoomSlots(room, parent2.getRoomSlots(room));
                offsprings[1].setRoomSlots(room, parent1.getRoomSlots(room));
            } else {
                // Copy from parent1 to offspring1, parent2 to offspring2
                offsprings[0].setRoomSlots(room, parent1.getRoomSlots(room));
                offsprings[1].setRoomSlots(room, parent2.getRoomSlots(room));
            }
        }
        
        return offsprings;
    }

    private Chromosome[] uniformCrossover(Chromosome parent1, Chromosome parent2) {
        Chromosome[] offsprings = { new Chromosome(), new Chromosome() };
        
        for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
            if (random.nextInt(2) == 1) {
                // Copy from parent1 to offspring1, parent2 to offspring2
                offsprings[0].setRoomSlots(room, parent1.getRoomSlots(room));
                offsprings[1].setRoomSlots(room, parent2.getRoomSlots(room));
            } else {
                // Copy from parent2 to offspring1, parent1 to offspring2
                offsprings[0].setRoomSlots(room, parent2.getRoomSlots(room));
                offsprings[1].setRoomSlots(room, parent1.getRoomSlots(room));
            }
        }
        
        return offsprings;
    }

    private Chromosome[] shuffleCrossover(Chromosome parent1, Chromosome parent2) {
        Chromosome[] offsprings = { new Chromosome(), new Chromosome() };
        
        // Randomly shuffle rooms
        List<Integer> rooms = new ArrayList<>();
        for (int i = 1; i <= Chromosome.NUM_ROOMS; i++) {
            rooms.add(i);
        }
        Collections.shuffle(rooms, random);

        // Randomly select crossover index
        int crossoverIndex = random.nextInt(Chromosome.NUM_ROOMS - 1) + 1;
        
        // Get the rooms to exchange
        List<Integer> exchangeRooms = rooms.subList(crossoverIndex, Chromosome.NUM_ROOMS);
        
        for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
            if (exchangeRooms.contains(room)) {
                // Copy from parent2 to offspring1, parent1 to offspring2
                offsprings[0].setRoomSlots(room, parent2.getRoomSlots(room));
                offsprings[1].setRoomSlots(room, parent1.getRoomSlots(room));
            } else {
                // Copy from parent1 to offspring1, parent2 to offspring2
                offsprings[0].setRoomSlots(room, parent1.getRoomSlots(room));
                offsprings[1].setRoomSlots(room, parent2.getRoomSlots(room));
            }
        }

        return offsprings;
    }

    /**
     * ======================
     * Mutation
     * ======================
     */
    
    // Does not check for prayer time constraint
    private void swapMutation(Chromosome chromosome) {
        // Pick a random position
        int pos1 = random.nextInt(Chromosome.TOTAL_GENES);

        // Pick another random position from a room of the same type
        Chromosome.SlotCoordinates coords1 = Chromosome.getCoordinates(pos1);
        Room room1 = roomCache.get(coords1.roomId);
        int pos2 = getRandomPosInRoomType(room1.getType());
        while (pos1 == pos2) {
            pos2 = getRandomPosInRoomType(room1.getType());
        }

        // Get genes
        int gene1 = chromosome.getGene(pos1);
        int gene2 = chromosome.getGene(pos2);

        // If both are empty or same class (overlapped)
        if (gene1 == gene2) return;

        Chromosome.Block block1 = chromosome.getBlock(pos1);
        Chromosome.Block block2 = chromosome.getBlock(pos2);

        // If at least one block is a class
        if (block1.length == block2.length) {
            swapEqualLengthBlocks(chromosome, block1, block2);
        } else if (block1.length < block2.length) {
            relocateShorterAndSwap(chromosome, block1, block2);
        } else {
            relocateShorterAndSwap(chromosome, block2, block1);
        }
    }

    /**
     * Get a random slot index in a random room of a given type
     */
    private int getRandomPosInRoomType(String roomType) {
        List<Integer> roomIds = roomsByRoomType.get(roomType);

        // Pick a random slot in a random room of this type
        int room = roomIds.get(random.nextInt(roomIds.size()));
        int day = random.nextInt(Chromosome.NUM_DAYS) + 1;
        int slot = random.nextInt(Chromosome.NUM_SLOTS) + 1;
        
        return Chromosome.getSlotIndex(room, day, slot);
    }

    /**
     * Swap two blocks of genes
     */
    private void swapEqualLengthBlocks(Chromosome chromosome, Chromosome.Block block1, Chromosome.Block block2) {
        for (int i = 0; i < block1.length; i++) {
            chromosome.setGene(block1.start + i, block2.gene);
            chromosome.setGene(block2.start + i, block1.gene);
        }
    }

    /**
     * Check if there are enough consecutive empty slots before a given position
     */
    private boolean checkSlotBefore(Chromosome chromosome, int start, int slotNeeded) { // start should be inclusive
        Chromosome.SlotCoordinates coords = Chromosome.getCoordinates(start);
        int startIndexOfDay = Chromosome.getStartIndexOfDay(coords.roomId, coords.dayId);
        if (start - slotNeeded < startIndexOfDay) return false;

        for (int i = 1; i <= slotNeeded; i++) {
            if (chromosome.getGene(start - i) != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if there are enough consecutive empty slots after a given position
     */
    private boolean checkSlotAfter(Chromosome chromosome, int end, int slotNeeded) { // end should be exclusive
        Chromosome.SlotCoordinates coords = Chromosome.getCoordinates(end - 1);
        int endIndexOfDay = Chromosome.getEndIndexOfDay(coords.roomId, coords.dayId);
        if (end + slotNeeded > endIndexOfDay) return false;

        for (int i = 0; i < slotNeeded; i++) {
            if (chromosome.getGene(end + i) != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Relocate a shorter block of genes and swap with a longer block of genes
     */
    private void relocateShorterAndSwap(Chromosome chromosome, Chromosome.Block shorter, Chromosome.Block longer) {
        int slotNeeded = longer.length - shorter.length;
        
        if (checkSlotBefore(chromosome, shorter.start, slotNeeded)) {
            setBlockOfGenes(chromosome, longer.gene, shorter.start - slotNeeded, longer.length);
            setBlockOfGenes(chromosome, 0, longer.start, slotNeeded);
            setBlockOfGenes(chromosome, shorter.gene, longer.start + slotNeeded, shorter.length);
        
        } else if (checkSlotAfter(chromosome, shorter.start + shorter.length, slotNeeded)) {
            setBlockOfGenes(chromosome, longer.gene, shorter.start, longer.length);
            setBlockOfGenes(chromosome, shorter.gene, longer.start, shorter.length);
            setBlockOfGenes(chromosome, 0, longer.start + shorter.length, slotNeeded);
        
        // If no space found, swap with an empty block
        } else {
            int roomId = Chromosome.getCoordinates(shorter.start).roomId;
            Chromosome.Block emptyBlock = chromosome.getEmptyBlockInRoom(roomId, longer.length);
            if (emptyBlock == null) return;
            swapEqualLengthBlocks(chromosome, emptyBlock, longer);
        }
    }

    /**
     * Set a block of genes
     */
    private void setBlockOfGenes(Chromosome chromosome, int gene, int start, int length) {
        for (int i = 0; i < length; i++) {
            chromosome.setGene(start + i, gene);
        }
    }

    // // Random positioning might not fit all classes in the day
    // private void uniformMutation(Chromosome chromosome) {
    //     for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
    //         for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
    //             if (random.nextDouble() < MUTATION_RATE) {
    //                 Set<Integer> classes = new HashSet<>();

    //                 // Get all classes on this day in this room
    //                 for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
    //                     int gene = chromosome.getGeneAt(room, day, slot);
    //                     if (gene != 0) {
    //                         classes.add(gene);
    //                     }
    //                 }

    //                 // Clear the day
    //                 clearDay(chromosome, room, day);

    //                 // Place the classes randomly
    //                 for (int classId : classes) {
    //                     int duration = classCache.get(classId).getDuration();
    //                     List<Integer> availableSlots = new ArrayList<>();

    //                     // Find all consecutive available slots in this day
    //                     for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
    //                         if (hasConsecutiveSlots(chromosome, room, day, slot, duration)) {
    //                             availableSlots.add(Chromosome.getSlotIndex(room, day, slot));
    //                         }
    //                     }

    //                     if (availableSlots.isEmpty()) continue;
    //                     int chosenSlot = availableSlots.get(random.nextInt(availableSlots.size()));
    //                     setBlockOfGenes(chromosome, classId, chosenSlot, duration);
    //                 }
    //             }
    //         }
    //     }
    // }

    // // Does not check for prayer time constraint
    // private void reverseMutation(Chromosome chromosome) {
    //     // Pick a random position
    //     int pos1 = random.nextInt(Chromosome.TOTAL_GENES);
    //     Chromosome.SlotCoordinates coords1 = Chromosome.getCoordinates(pos1);
        
    //     // Pick another random position from the same room
    //     int pos2 = getRandomPosInRoom(coords1.roomId);
    //     while (pos1 == pos2) {
    //         pos2 = getRandomPosInRoom(coords1.roomId);
    //     }

    //     // Ensure pos1 is less than pos2
    //     if (pos1 > pos2) {
    //         int temp = pos1;
    //         pos1 = pos2;
    //         pos2 = temp;
    //     }

    //     // Update coordinates after potential swap
    //     coords1 = Chromosome.getCoordinates(pos1);
    //     Chromosome.SlotCoordinates coords2 = Chromosome.getCoordinates(pos2);

    //     // Find class boundaries for both positions
    //     // For pos1: find start of class
    //     int gene1 = chromosome.getGene(pos1);
    //     int dayStart1 = Chromosome.getStartIndexOfDay(coords1.roomId, coords1.dayId);
    //     if (gene1 != 0) {
    //         while (pos1 > dayStart1 && gene1 == chromosome.getGene(pos1 - 1)) {
    //             pos1--;
    //         }
    //     }

    //     // For pos2: find end of class
    //     int gene2 = chromosome.getGene(pos2);
    //     int dayEnd2 = Chromosome.getEndIndexOfDay(coords2.roomId, coords2.dayId);
    //     if (gene2 != 0) {
    //         while (pos2 < dayEnd2 - 1 && gene2 == chromosome.getGene(pos2 + 1)) {
    //             pos2++;
    //         }
    //     }

    //     // Update coordinates after index adjustment
    //     coords1 = Chromosome.getCoordinates(pos1);
    //     coords2 = Chromosome.getCoordinates(pos2);

    //     // Reverse the genes day by day
    //     for (int day = coords1.dayId; day <= coords2.dayId; day++) {
    //         int dayStart = Chromosome.getStartIndexOfDay(coords1.roomId, day);
    //         int dayEnd = Chromosome.getEndIndexOfDay(coords1.roomId, day) - 1; // make it inclusive

    //         if (dayStart < pos1) dayStart = pos1;
    //         if (dayEnd > pos2) dayEnd = pos2;

    //         // Reverse the block of genes
    //         int length = dayEnd - dayStart + 1;
    //         for (int i = 0; i < length / 2; i++) {
    //             int gene = chromosome.getGene(dayStart + i);
    //             chromosome.setGene(dayStart + i, chromosome.getGene(dayEnd - i));
    //             chromosome.setGene(dayEnd - i, gene);
    //         }
    //     }
    // }

    // /**
    //  * Get a random slot index in a random day of a given room
    //  */
    // private int getRandomPosInRoom(int roomId) {
    //     int day = random.nextInt(Chromosome.NUM_DAYS) + 1;
    //     int slot = random.nextInt(Chromosome.NUM_SLOTS) + 1;
    //     return Chromosome.getSlotIndex(roomId, day, slot);
    // }

    /**
     * ======================
     * Repair
     * ======================
     */

    /**
     * Repair chromosome to ensure no missing or duplicate classes
     */
    private void repairChromosome(Chromosome chromosome) {
        // Compute class occurrences
        Map<Integer, Integer> classOccurrences = new HashMap<>();
        for (int i = 0; i < Chromosome.TOTAL_GENES; i++) {
            int classId = chromosome.getGene(i);
            if (classId > 0) {
                classOccurrences.put(classId, classOccurrences.getOrDefault(classId, 0) + 1);
            }
        }

        // Find classes to add
        Set<Integer> classesToAdd = new HashSet<>();
        for (Class classObj : allClasses) {
            int classId = classObj.getId();
            int occurrences = classOccurrences.getOrDefault(classId, 0);

            // // Missing class
            // if (occurrences == 0) {
            //     classesToAdd.add(classId);
            
            // // Duplicate class
            // } else if (occurrences != classObj.getDuration()) {
            //     removeOccurrences(chromosome, classId);
            //     classesToAdd.add(classId);
            // }

            if (occurrences != classObj.getDuration()) {
                removeOccurrences(chromosome, classId);
                classesToAdd.add(classId);
            }
        }

        // Enforce Friday prayer time constraint
        for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
            for (int slot = 5; slot <= 6; slot++) {
                int gene = chromosome.getGeneAt(room, 5, slot);
                if (gene != 0) {
                    removeOccurrences(chromosome, gene);
                    classesToAdd.add(gene);
                }
            }
        }

        // Enforce no cross-day boundary
        for (int room = 1; room <= Chromosome.NUM_ROOMS; room++) {
            for (int day = 1; day <= Chromosome.NUM_DAYS - 1; day++) {
                int endGene = chromosome.getGeneAt(room, day, Chromosome.NUM_SLOTS);
                if (endGene == 0) continue;
                int startNext = chromosome.getGeneAt(room, day + 1, 1);
                if (startNext == endGene) {
                    removeOccurrences(chromosome, endGene);
                    classesToAdd.add(endGene);
                }
            }
        }

        // Place violated classes
        for (int classId : classesToAdd) {
            Class classObj = classCache.get(classId);
            if (!placeClassRandomly(chromosome, classObj)) relocateClass(chromosome, classObj);
        }
    }
    
    /**
     * Remove occurrences of a class from the chromosome
     */
    private void removeOccurrences(Chromosome chromosome, int classId) {
        for (int i = 0; i < Chromosome.TOTAL_GENES; i++) {
            if (chromosome.getGene(i) == classId) {
                chromosome.setGene(i, 0);
            }
        }
    }

    /**
     * Try progressively-more-expensive relocations to make room for target class:
     *  1) Re-pack same room & day
     *  2) Re-pack same room across days
     *  3) Re-pack rooms of same type across days
     */
    private void relocateClass(Chromosome chromosome, Class classObj) {
        List<Integer> compatibleRooms = roomsByClassType.get(classObj.getType());
    
        for (int roomId : compatibleRooms) {
            // 1) Same-day attempt
            for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
                if (relocateClassesOnSameDay(chromosome, classObj.getId(), roomId, day)) return;
            }
            // 2) Same-room attempt
            if (relocateClassesOnSameRoom(chromosome, classObj.getId(), roomId)) return;
        }
        // 3) Same-room-type attempt
        relocateClassesOnSameRoomType(chromosome, classObj.getId(), compatibleRooms);
    }

    /**
     * Relocate classes on the same day
     */
    private boolean relocateClassesOnSameDay(Chromosome chromosome, int targetClassId, int roomId, int day) {
        // Get all classes on this day in this room
        Set<Integer> classes = new HashSet<>();
        for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
            int gene = chromosome.getGeneAt(roomId, day, slot);
            if (gene != 0) {
                classes.add(gene);
            }
        }

        // Add the current class
        classes.add(targetClassId);

        // Check capacity
        if (getTotalDuration(classes) > getCapacityByDay(day)) return false;

        // Clear the day
        clearDay(chromosome, roomId, day);

        // Get allowed segments for this day
        List<int[]> segments = getSegmentsForDay(day);

        // Place across allowed segments
        int segIdx = 0, cursor = segments.get(0)[0], segEnd = segments.get(0)[1];
        for (int classId : classes) {
            int len = classCache.get(classId).getDuration();

            // Advance to a segment that can hold 'len'
            while (segIdx < segments.size() && (cursor + len - 1 > segEnd)) {
                segIdx++;
                if (segIdx < segments.size()) {
                    cursor = segments.get(segIdx)[0];
                    segEnd = segments.get(segIdx)[1];
                } else {
                    return false;
                }
            }

            // Place the class
            int start = Chromosome.getSlotIndex(roomId, day, cursor);
            setBlockOfGenes(chromosome, classId, start, len);
            cursor += len;
        }

        return true;
    }

    /**
     * Relocate classes on the same room
     */
    private boolean relocateClassesOnSameRoom(Chromosome chromosome, int targetClassId, int roomId) {
        // Get all classes in this room across all days
        Set<Integer> classesSet = new HashSet<>();
        for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
            for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                int gene = chromosome.getGeneAt(roomId, day, slot);
                if (gene != 0) {
                    classesSet.add(gene);
                }
            }
        }

        // Add the current class
        classesSet.add(targetClassId);

        // Check capacity
        int capacity = 0;
        for (int day = 1; day <= Chromosome.NUM_DAYS; day++) capacity += getCapacityByDay(day);
        if (getTotalDuration(classesSet) > capacity) return false;

        // Clear the room
        for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
            clearDay(chromosome, roomId, day);
        }

        Integer[] classes = classesSet.toArray(new Integer[0]);
        int allocated = 0;
        for (int day = 1; day <= Chromosome.NUM_DAYS && allocated < classes.length; day++) {
            // Get allowed segments for this day
            List<int[]> segments = getSegmentsForDay(day);

            // Place across allowed segments
            int segIdx = 0, cursor = segments.get(0)[0], segEnd = segments.get(0)[1];
            while (allocated < classes.length) {
                int classId = classes[allocated];
                int len = classCache.get(classId).getDuration();
                
                // Advance to a segment that can hold 'len'
                while (segIdx < segments.size() && (cursor + len - 1 > segEnd)) {
                    segIdx++;
                    if (segIdx < segments.size()) {
                        cursor = segments.get(segIdx)[0];
                        segEnd = segments.get(segIdx)[1];
                    }
                }
                if (segIdx >= segments.size()) break; // Move to next day

                // Place the class
                int start = Chromosome.getSlotIndex(roomId, day, cursor);
                setBlockOfGenes(chromosome, classId, start, len);
                cursor += len;
                allocated++;
            }
        }

        return allocated == classes.length;
    }

    /**
     * Relocate classes on the same room type
     */
    private boolean relocateClassesOnSameRoomType(Chromosome chromosome, int targetClassId, List<Integer> rooms) {
        // Get all classes in rooms of this type across all days
        Set<Integer> classesSet = new HashSet<>();
        for (int roomId : rooms) {
            for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
                for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
                    int gene = chromosome.getGeneAt(roomId, day, slot);
                    if (gene != 0) {
                        classesSet.add(gene);
                    }
                }
            }
        }

        // Add the current class
        classesSet.add(targetClassId);

        // Check capacity
        int capacity = 0;
        for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
            capacity += getCapacityByDay(day) * rooms.size();
        }
        if (getTotalDuration(classesSet) > capacity) return false;

        // Clear the rooms
        for (int roomId : rooms) {
            for (int day = 1; day <= Chromosome.NUM_DAYS; day++) {
                    clearDay(chromosome, roomId, day);
                }
        }

        Integer[] classes = classesSet.toArray(new Integer[0]);
        int allocated = 0;
        for (int roomId : rooms) {
            for (int day = 1; day <= Chromosome.NUM_DAYS && allocated < classes.length; day++) {
                // Get allowed segments for this day
                List<int[]> segments = getSegmentsForDay(day);

                // Place across allowed segments
                int segIdx = 0, cursor = segments.get(0)[0], segEnd = segments.get(0)[1];
                while (allocated < classes.length) {
                    int classId = classes[allocated];
                    int len = classCache.get(classId).getDuration();
                    
                    // Advance to a segment that can hold 'len'
                    while (segIdx < segments.size() && (cursor + len - 1 > segEnd)) {
                        segIdx++;
                        if (segIdx < segments.size()) {
                            cursor = segments.get(segIdx)[0];
                            segEnd = segments.get(segIdx)[1];
                        }
                    }
                    if (segIdx >= segments.size()) break; // Move to next day

                    // Place the class
                    int start = Chromosome.getSlotIndex(roomId, day, cursor);
                    setBlockOfGenes(chromosome, classId, start, len);
                    cursor += len;
                    allocated++;
                }
            }
            if (allocated >= classes.length) break;
        }

        return allocated == classes.length;
    }

    /**
     * Get the total duration of a set of classes
     */
    private int getTotalDuration(Set<Integer> classIds) {
        int totalDuration = 0;
        for (int id : classIds) {
            totalDuration += classCache.get(id).getDuration();
        }
        return totalDuration;
    }

    /**
     * Get the capacity for a given day
     */
    private static int getCapacityByDay(int day) {
        // Skip Friday prayer time
        // At worst case, slot 3 and slot 4 are skipped
        // and 2 hours of classes might have no place to fit in
        // Might have edge case e.g. 3 + 3 + 2 hours of lectures
        // while satisfying the capacity limit but unable to fit in the structure
        // since these classes can't form 2 4-hour segments
        return day == 5 ? Chromosome.NUM_SLOTS - 4 : Chromosome.NUM_SLOTS;
    }

    /**
     * Get a list of segments for a given day
     * Each int[] is [startSlot, endSlot] inclusive
     */
    private List<int[]> getSegmentsForDay(int day) {
        if (day == 5) {
            // Skip slots 5 and 6
            return Arrays.asList(new int[]{1, 4}, new int[]{7, Chromosome.NUM_SLOTS});
        }
        return Collections.singletonList(new int[]{1, Chromosome.NUM_SLOTS});
    }

    /**
     * Clear a day
     */
    private void clearDay(Chromosome chromosome, int roomId, int day) {
        for (int slot = 1; slot <= Chromosome.NUM_SLOTS; slot++) {
            chromosome.setGeneAt(roomId, day, slot, 0);
        }
    }

    /**
     * ======================
     * Replacement
     * ======================
     */

    private void weakParentReplacement(List<Chromosome> population, Chromosome parent1, Chromosome parent2, Chromosome offspring1, Chromosome offspring2) {
        // Build the candidate pool
        List<Chromosome> candidates = new ArrayList<>(4);
        candidates.add(parent1);
        candidates.add(parent2);
        candidates.add(offspring1);
        candidates.add(offspring2);

        // Sort the candidates by fitness
        Comparator<Chromosome> byFitness = Comparator.comparingInt(Chromosome::getFitness);
        candidates.sort(byFitness); // ascending => best first

        // Replace the 2 parents with the best 2 candidates
        population.set(population.indexOf(parent1), candidates.get(0));
        if (parent1 != parent2) population.set(population.indexOf(parent2), candidates.get(1)); // Handle the case where both parents are the same
    }

    // private void randomReplacement(List<Chromosome> population, Chromosome parent1, Chromosome parent2, Chromosome offspring1, Chromosome offspring2) {
    //     // Randomly select 2 individuals to replace
    //     int replacement1 = random.nextInt(population.size());
    //     int replacement2 = random.nextInt(population.size());
    //     while (replacement1 == replacement2) {
    //         replacement2 = random.nextInt(population.size());
    //     }
        
    //     // Replace the 2 individuals with the 2 offspring
    //     population.set(replacement1, offspring1);
    //     population.set(replacement2, offspring2);
    // }

    private void binaryTournamentReplacement(List<Chromosome> population, Chromosome parent1, Chromosome parent2, Chromosome offspring1, Chromosome offspring2) {
        // Tournament 1: Find worst individual to replace
        int candidate1 = random.nextInt(population.size());
        int candidate2 = random.nextInt(population.size());
        while (candidate1 == candidate2) {
            candidate2 = random.nextInt(population.size());
        }
        
        // Select the worse candidate as replacement
        int replacement1 = (population.get(candidate1).getFitness() > population.get(candidate2).getFitness()) 
                          ? candidate1 : candidate2;

        // Tournament 2: Find another worst individual to replace
        candidate1 = random.nextInt(population.size());
        while (candidate1 == replacement1) {
            candidate1 = random.nextInt(population.size());
        }
        candidate2 = random.nextInt(population.size());
        while (candidate1 == candidate2 || candidate2 == replacement1) {
            candidate2 = random.nextInt(population.size());
        }
        
        // Select the worse candidate as replacement
        int replacement2 = (population.get(candidate1).getFitness() > population.get(candidate2).getFitness()) 
                          ? candidate1 : candidate2;

        // Replace the selected individuals with offspring
        population.set(replacement1, offspring1);
        population.set(replacement2, offspring2);
    }

    // private void bothParentReplacement(List<Chromosome> population, Chromosome parent1, Chromosome parent2, Chromosome offspring1, Chromosome offspring2) {
    //     // Replace the 2 parents with the 2 offspring
    //     population.set(population.indexOf(parent1), offspring1);
    //     if (parent1 != parent2) population.set(population.indexOf(parent2), offspring2); // Handle the case where both parents are the same
    // }

    private void linearRankingReplacement(List<Chromosome> population, Chromosome parent1, Chromosome parent2, Chromosome offspring1, Chromosome offspring2) {
        Chromosome[] replacements = new Chromosome[2];
        
        // Sort the population by fitness (descending - worse fitness first)
        List<Chromosome> sorted = new ArrayList<>(population);
        sorted.sort(Comparator.comparingInt(Chromosome::getFitness).reversed());

        // Calculate total selection weight
        // Linear ranking: rank 1 gets highest weight, rank n gets lowest weight
        int totalWeight = 0;
        for (int i = 0; i < sorted.size(); i++) {
            totalWeight += (sorted.size() - i);
        }

        // Select the 2 chromosomes to replace
        int randomValue = random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (int j = 0; j < sorted.size(); j++) {
            cumulativeWeight += (sorted.size() - j);
            if (cumulativeWeight > randomValue) {
                replacements[0] = sorted.get(j);
                break;
            }
        }

        boolean found = false;
        do {
            randomValue = random.nextInt(totalWeight);
            cumulativeWeight = 0;

            for (int j = 0; j < sorted.size(); j++) {
                cumulativeWeight += (sorted.size() - j);
                if (cumulativeWeight > randomValue) {
                    if (sorted.get(j) == replacements[0]) break;
                    replacements[1] = sorted.get(j);
                    found = true;
                    break;
                }
            }
        } while (!found);

        // Replace the 2 chromosomes with the offspring
        population.set(population.indexOf(replacements[0]), offspring1);
        population.set(population.indexOf(replacements[1]), offspring2);
    }

    private void weakChromosomeReplacement(List<Chromosome> population, Chromosome parent1, Chromosome parent2, Chromosome offspring1, Chromosome offspring2) {
        Comparator<Chromosome> byFitness = Comparator.comparingInt(Chromosome::getFitness);
        
        // Find the first worst chromosome
        Chromosome worst1 = Collections.max(population, byFitness);
        
        // Find the second worst chromosome
        Chromosome worst2 = null;
        for (Chromosome chromosome : population) {
            if (chromosome != worst1) {
                if (worst2 == null || chromosome.getFitness() > worst2.getFitness()) {
                    worst2 = chromosome;
                }
            }
        }

        // Build the candidate pool
        List<Chromosome> candidates = new ArrayList<>(4);
        candidates.add(worst1);
        candidates.add(worst2);
        candidates.add(offspring1);
        candidates.add(offspring2);

        // Sort the candidates by fitness (ascending - best first)
        candidates.sort(byFitness);

        // Replace the 2 worst chromosomes with the best 2 candidates
        population.set(population.indexOf(worst1), candidates.get(0));
        population.set(population.indexOf(worst2), candidates.get(1));
    }

    /**
     * ======================
     */

    /**
     * Get the best chromosome from population
     */
    private Chromosome getBestChromosome(List<Chromosome> population) {
        Comparator<Chromosome> byFitness = Comparator.comparingInt(Chromosome::getFitness);
        return Collections.min(population, byFitness);
    }
    
    /**
     * Save algorithm statistics
     */
    private void saveStatistics(int exp, long totalTime, Chromosome bestChromosome) throws IOException {
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File expDir = new File(outputDir, Integer.toString(exp));
        if (!expDir.exists()) {
            expDir.mkdirs();
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter("output/" + exp + "/statistics.txt"))) {
            writer.println("Genetic Algorithm Statistics");
            writer.println("============================");
            writer.printf("Population Size: %d\n", POPULATION_SIZE);
            writer.printf("Number of Generations: %d\n", numGenerations);
            writer.printf("Crossover Rate: %.2f\n", CROSSOVER_RATE);
            writer.printf("Mutation Rate: %.2f\n", MUTATION_RATE);
            writer.printf("Total Time (seconds): %.2f\n", (double) totalTime / 1000000000.0);
            writer.printf("Final Best Fitness: %d\n", bestChromosome.getFitness());
            writer.println();
            writer.println("Best Fitness per Generation:");
            
            for (int i = 0; i < bestFitnessPerGeneration.size(); i++) {
                writer.printf("Generation %d: %d\n", i + 1, bestFitnessPerGeneration.get(i));
            }
        }
        
        System.out.println("Statistics saved to output/" + exp + "/statistics.txt");
    }
}
