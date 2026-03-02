import java.util.*;

/**
 * Represents a chromosome in the genetic algorithm
 * A chromosome is a complete timetable solution with 250 genes (5 rooms × 5 days × 10 slots)
 */
public class Chromosome {
    public static final int NUM_ROOMS = 5;
    public static final int NUM_DAYS = 5;
    public static final int NUM_SLOTS = 10;
    public static final int TOTAL_GENES = NUM_ROOMS * NUM_DAYS * NUM_SLOTS; // 250
    
    private int[] genes;
    private int fitness; // Fitness score (penalty cost)
    
    /**
     * Constructor - creates a chromosome with empty slots
     */
    public Chromosome() {
        this.genes = new int[TOTAL_GENES];
        this.fitness = Integer.MAX_VALUE;

        // Initialize all slots as empty (0)
        Arrays.fill(genes, 0);
    }
    
    /**
     * Copy constructor
     */
    public Chromosome(Chromosome other) {
        this.genes = other.genes.clone();
        this.fitness = other.fitness;
    }
    
    /**
     * Get the gene value at a specific index
     */
    public int getGene(int index) {
        return genes[index];
    }
    
    /**
     * Set the gene value at a specific index
     */
    public void setGene(int index, int value) {
        genes[index] = value;
    }
    
    /**
     * Get all genes
     */
    public int[] getGenes() {
        return genes.clone();
    }
    
    /**
     * Set all genes
     */
    public void setGenes(int[] newGenes) {
        this.genes = newGenes.clone();
    }
    
    /**
     * Get fitness value
     */
    public int getFitness() {
        return fitness;
    }
    
    /**
     * Set fitness value
     */
    public void setFitness(int fitness) {
        this.fitness = fitness;
    }
    
    /**
     * Convert slot index to room, day, and slot coordinates
     */
    public static class SlotCoordinates {
        public final int roomId;
        public final int dayId;
        public final int slotId;
        
        public SlotCoordinates(int roomId, int dayId, int slotId) {
            this.roomId = roomId;
            this.dayId = dayId;
            this.slotId = slotId;
        }
        
        @Override
        public String toString() {
            return String.format("Room %d, Day %d, Slot %d", roomId, dayId, slotId);
        }
    }
    
    /**
     * Get coordinates from slot index
     */
    public static SlotCoordinates getCoordinates(int slotIndex) {
        int slotId = (slotIndex % (NUM_DAYS * NUM_SLOTS)) % NUM_SLOTS + 1;
        int roomId = slotIndex / (NUM_DAYS * NUM_SLOTS) + 1;
        int dayId = (slotIndex % (NUM_DAYS * NUM_SLOTS)) / NUM_SLOTS + 1;
        
        return new SlotCoordinates(roomId, dayId, slotId);
    }
    
    /**
     * Get slot index from coordinates
     */
    public static int getSlotIndex(int roomId, int dayId, int slotId) {
        return ((roomId - 1) * (NUM_DAYS * NUM_SLOTS)) + ((dayId - 1) * NUM_SLOTS) + slotId - 1;
    }
    
    /**
     * Get the gene value at specific coordinates
     */
    public int getGeneAt(int roomId, int dayId, int slotId) {
        int index = getSlotIndex(roomId, dayId, slotId);
        return genes[index];
    }
    
    /**
     * Set the gene value at specific coordinates
     */
    public void setGeneAt(int roomId, int dayId, int slotId, int value) {
        int index = getSlotIndex(roomId, dayId, slotId);
        setGene(index, value);
    }

    /**
     * Represents a block of genes (a class or an empty slot)
     */
    public class Block {
        public final int gene;
        public final int start;
        public final int length;

        public Block(int gene, int start, int length) {
            this.gene = gene;
            this.start = start;
            this.length = length;
        }

        @Override
        public String toString() {
            return String.format("Block{gene=%d, start=%d, length=%d}", gene, start, length);
        }
    }

    /**
     * Get a block of genes with a given slot index
     */
    public Block getBlock(int slotIndex) {
        int gene = genes[slotIndex];
        if (gene == 0) return new Block(gene, slotIndex, 1);

        SlotCoordinates coords = getCoordinates(slotIndex);
        int dayStart = getStartIndexOfDay(coords.roomId, coords.dayId);
        int dayEnd = getEndIndexOfDay(coords.roomId, coords.dayId);

        // Get the start index of the block
        int start = slotIndex;
        while (start > dayStart && gene == genes[start - 1]) {
            start--;
        }

        // Get the length of the block
        int length = 1;
        while (start + length < dayEnd && gene == genes[start + length]) {
            length++;
        }

        return new Block(gene, start, length);
    }
    
    /**
     * Get an empty block of genes in a given room with a given length
     */
    public Block getEmptyBlockInRoom(int room, int length) {
        for (int day = 1; day <= NUM_DAYS; day++) {
            int dayStart = getStartIndexOfDay(room, day);
            int dayEnd = getEndIndexOfDay(room, day);
            int cnt = 0, blockStart = dayStart;

            for (int slot = dayStart; slot < dayEnd; slot++) {
                if (genes[slot] == 0) {
                    if (cnt == 0) blockStart = slot;
                    if (++cnt == length) {
                        return new Block(0, blockStart, length);
                    }
                } else {
                    cnt = 0;
                }
            }
        }

        return null;
    }
    
    /**
     * Get the start index of a day (inclusive)
     */
    public static int getStartIndexOfDay(int room, int day) {
        return (room - 1) * (NUM_DAYS * NUM_SLOTS) + (day - 1) * NUM_SLOTS;
    }

    /**
     * Get the end index of a day (exclusive)
     */
    public static int getEndIndexOfDay(int room, int day) {
        return getStartIndexOfDay(room, day) + NUM_SLOTS;
    }

    /**
     * Get all classes scheduled in the timetable
     */
    public Set<Integer> getScheduledClasses() {
        Set<Integer> classes = new HashSet<>();
        for (int gene : genes) {
            if (gene > 0) {
                classes.add(gene);
            }
        }
        return classes;
    }
    
    /**
     * Find empty slots (genes with value 0)
     */
    public List<Integer> getEmptySlots() {
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < genes.length; i++) {
            if (genes[i] == 0) {
                emptySlots.add(i);
            }
        }
        return emptySlots;
    }
    
    /**
     * Get slots for a specific room
     */
    public int[] getRoomSlots(int roomId) {
        int startIndex = (roomId - 1) * (NUM_DAYS * NUM_SLOTS);
        int endIndex = startIndex + (NUM_DAYS * NUM_SLOTS);
        return Arrays.copyOfRange(genes, startIndex, endIndex);
    }
    
    /**
     * Set slots for a specific room
     */
    public void setRoomSlots(int roomId, int[] roomSlots) {
        int startIndex = (roomId - 1) * (NUM_DAYS * NUM_SLOTS);
        System.arraycopy(roomSlots, 0, genes, startIndex, NUM_DAYS * NUM_SLOTS);
    }

    /**
     * Check if a slot is a prayer slot
     */
    public static boolean isPrayerSlot(int day, int slot) {
        return day == 5 && (slot == 5 || slot == 6);
    }
    
    @Override
    public String toString() {
        return String.format("Chromosome{fitness=%d}", fitness);
    }
}
