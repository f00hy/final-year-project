# Final Year Project

Report: [Evaluate the Performance of University Course Timetabling Problem with Different Combinations of Genetic Algorithm](http://eprints.utar.edu.my/7101/)

### Database Setup

1. Import `sql/database_schema.sql` into MySQL to create the `university_timetabling` database and all required tables.
2. After that, import `sql/data_generation.sql` into the same database to populate it with the sample data used in this project.

### Data Reference

The file `sql/data.txt` documents the course, lecturer, and class information that is encoded in:
- `sql/data_generation.sql` (static SQL inserts), and
- `src/DataGenerator.java` (logic that generates classes and group–class assignments).

### Generating Classes and Group Assignments (First Run Only)

On the **first run**, you must generate classes and group–class assignments via `DataGenerator`:
1. Open `src/Main.java`.
2. Uncomment the block that creates a `DataGenerator` and calls:
   - `generator.generateClasses();`
   - `generator.generateGroupClassAssignments();`
3. Run the program once to generate the necessary data.
4. After the first successful generation, **comment this block back out** to avoid regenerating or duplicating data on subsequent runs.

### Running Genetic Algorithm Experiments

The Java entry point is `src/Main.java`. When run in its default (post–data-generation) state:
1. It executes the genetic algorithm **10 times** using:
   - `for (int i = 1; i <= 10; i++) { ... }`
2. For each run `i`, it:
   - Runs `GeneticAlgorithm`,
   - Writes `statistics.txt` and timetable outputs under `output/i/`.

### Summarizing Results

After `Main.java` finishes all 10 runs and the `output/1` to `output/10` folders exist:
1. Run `scripts/results_generation.py`.
2. The script reads each `output/i/statistics.txt` file,
3. Computes summary statistics across the experiments, and
4. Writes a consolidated CSV file to `output/results.csv`.

### Trying Different Genetic Algorithm Operator Combinations

Different selection, crossover, mutation, and replacement operators are implemented in `src/GeneticAlgorithm.java`.  
To evaluate different combinations:
- Manually change which selection method is used (e.g. `binaryTournamentSelection`, `rouletteWheelSelection`, etc.).
- Manually change which crossover operator is used (e.g. `uniformCrossover`, `singlePointCrossover`, etc.).
- Manually change which replacement strategy is used (e.g. `weakChromosomeReplacement`, `weakParentReplacement`, etc.).
- Rebuild and rerun `Main.java` for each combination to collect new results.
