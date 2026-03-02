import os
import csv

def extract_value_from_line(line):
    """Extract the last numeric value from a line"""
    value = line.split()[-1]
    return float(value) if '.' in value else int(value)

def parse_statistics_file(filepath):
    """Parse statistics.txt file and extract required metrics"""
    try:
        with open(filepath, 'r') as file:
            lines = file.readlines()
            
        # Extract values from specific lines (0-indexed)
        num_generations = extract_value_from_line(lines[3])  # Line 4
        total_time = extract_value_from_line(lines[6])       # Line 7
        initial_fitness = extract_value_from_line(lines[10]) # Line 11
        
        # Calculate fitness improvement per generation
        fitness_improvement = initial_fitness / num_generations if num_generations > 0 else 0
        
        return {
            'num_generations': num_generations,
            'total_time': total_time,
            'initial_fitness': initial_fitness,
            'fitness_improvement': fitness_improvement
        }
    except Exception as e:
        print(f"Error parsing {filepath}: {e}")
        return None

def main():
    output_dir = "output"
    results = []
    
    # Process folders 1 to 10
    for i in range(1, 11):
        folder_path = os.path.join(output_dir, str(i))
        stats_file = os.path.join(folder_path, "statistics.txt")
        
        if os.path.exists(stats_file):
            data = parse_statistics_file(stats_file)
            if data:
                results.append({
                    'experiment': i,
                    **data
                })
                print(f"Processed experiment {i}: {data}")
            else:
                print(f"Failed to parse experiment {i}")
        else:
            print(f"Statistics file not found for experiment {i}: {stats_file}")
    
    if not results:
        print("No valid results found!")
        return
    
    # Calculate averages
    num_experiments = len(results)
    avg_generations = sum(r['num_generations'] for r in results) / num_experiments
    avg_time = sum(r['total_time'] for r in results) / num_experiments
    avg_initial_fitness = sum(r['initial_fitness'] for r in results) / num_experiments
    avg_fitness_improvement = sum(r['fitness_improvement'] for r in results) / num_experiments
    
    # Write CSV file
    csv_path = os.path.join(output_dir, "results.csv")
    with open(csv_path, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        
        # Write header
        writer.writerow(['Experiment', 'Number of Generations', 'Total Time', 'Initial Fitness', 'Fitness Improvement per Generation'])
        
        # Write experiment data
        for result in results:
            writer.writerow([
                result['experiment'],
                result['num_generations'],
                f"{result['total_time']:.2f}",
                result['initial_fitness'],
                f"{result['fitness_improvement']:.2f}"
            ])
        
        # Write average row
        writer.writerow([
            'Average',
            f"{avg_generations:.2f}",
            f"{avg_time:.2f}",
            f"{avg_initial_fitness:.2f}",
            f"{avg_fitness_improvement:.2f}"
        ])
    
    print(f"\nResults written to {csv_path}")
    print(f"Processed {num_experiments} experiments")
    print(f"Average generations: {avg_generations:.2f}")
    print(f"Average time: {avg_time:.2f} seconds")
    print(f"Average initial fitness: {avg_initial_fitness:.2f}")
    print(f"Average fitness improvement per generation: {avg_fitness_improvement:.2f}")

if __name__ == "__main__":
    main()
