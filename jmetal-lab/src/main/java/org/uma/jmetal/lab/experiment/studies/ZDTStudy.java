package org.uma.jmetal.lab.experiment.studies;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.moead.AbstractMOEAD;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADBuilder;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.legacy.NSGAIIBuilder;
import org.uma.jmetal.algorithm.multiobjective.smpso.SMPSOBuilder;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.component.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.operator.crossover.impl.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.qualityindicator.impl.*;
import org.uma.jmetal.qualityindicator.impl.hypervolume.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.archive.impl.CrowdingDistanceArchive;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Example of experimental study based on solving the ZDT problems with the algorithms NSGAII,
 * MOEA/D, and SMPSO
 * <p>
 * This org.uma.jmetal.experiment assumes that the reference Pareto front are known and that, given a problem named
 * P, there is a corresponding file called P.pf containing its corresponding Pareto front. If this
 * is not the case, please refer to class {@link DTLZStudy} to see an example of how to explicitly
 * indicate the name of those files.
 * <p>
 * Six quality indicators are used for performance assessment.
 * <p>
 * The steps to carry out the org.uma.jmetal.experiment are: 1. Configure the org.uma.jmetal.experiment 2. Execute the algorithms
 * 3. Compute que quality indicators 4. Generate Latex tables reporting means and medians 5.
 * Generate R scripts to produce latex tables with the result of applying the Wilcoxon Rank Sum Test
 * 6. Generate Latex tables with the ranking obtained by applying the Friedman test 7. Generate R
 * scripts to obtain boxplots
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */

public class ZDTStudy {

  private static final int INDEPENDENT_RUNS = 25;

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new JMetalException("Missing argument: experimentBaseDirectory");
    }
    String experimentBaseDirectory = args[0];

    List<ExperimentProblem<DoubleSolution>> problemList = new ArrayList<>();
    problemList.add(new ExperimentProblem<>(new ZDT1()));
    problemList.add(new ExperimentProblem<>(new ZDT2()));
    problemList.add(new ExperimentProblem<>(new ZDT3()));
    problemList.add(new ExperimentProblem<>(new ZDT4()));
    problemList.add(new ExperimentProblem<>(new ZDT6()));

    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithmList =
            configureAlgorithmList(problemList);

    Experiment<DoubleSolution, List<DoubleSolution>> experiment =
            new ExperimentBuilder<DoubleSolution, List<DoubleSolution>>("ZDTStudy")
                    .setAlgorithmList(algorithmList)
                    .setProblemList(problemList)
                    .setReferenceFrontDirectory("/pareto_fronts")
                    .setExperimentBaseDirectory(experimentBaseDirectory)
                    .setOutputParetoFrontFileName("FUN")
                    .setOutputParetoSetFileName("VAR")
                    .setIndicatorList(Arrays.asList(
                            new Epsilon<DoubleSolution>(),
                            new Spread<DoubleSolution>(),
                            new GenerationalDistance<DoubleSolution>(),
                            new PISAHypervolume<DoubleSolution>(),
                            new InvertedGenerationalDistance<DoubleSolution>(),
                            new InvertedGenerationalDistancePlus<DoubleSolution>()))
                    .setIndependentRuns(INDEPENDENT_RUNS)
                    .setNumberOfCores(8)
                    .build();

    //new ExecuteAlgorithms<>(org.uma.jmetal.experiment).run();
    new ComputeQualityIndicators<>(experiment).run();
    /*
    new GenerateLatexTablesWithStatistics(org.uma.jmetal.experiment).run();
    new GenerateWilcoxonTestTablesWithR<>(org.uma.jmetal.experiment).run();
    new GenerateFriedmanTestTables<>(org.uma.jmetal.experiment).run();
    new GenerateBoxplotsWithR<>(org.uma.jmetal.experiment).setRows(2).setColumns(3).run();
    */
  }

  /**
   * The algorithm list is composed of pairs {@link Algorithm} + {@link Problem} which form part of
   * a {@link ExperimentAlgorithm}, which is a decorator for class {@link Algorithm}.
   */
  static List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> configureAlgorithmList(
          List<ExperimentProblem<DoubleSolution>> problemList) {
    List<ExperimentAlgorithm<DoubleSolution, List<DoubleSolution>>> algorithms = new ArrayList<>();
    for (int run = 0; run < INDEPENDENT_RUNS; run++) {

      for (int i = 0; i < problemList.size(); i++) {
        double mutationProbability = 1.0 / problemList.get(i).getProblem().getNumberOfVariables();
        double mutationDistributionIndex = 20.0;
        Algorithm<List<DoubleSolution>> algorithm = new SMPSOBuilder(
                (DoubleProblem) problemList.get(i).getProblem(),
                new CrowdingDistanceArchive<DoubleSolution>(100))
                .setMutation(new PolynomialMutation(mutationProbability, mutationDistributionIndex))
                .setMaxIterations(250)
                .setSwarmSize(100)
                .setSolutionListEvaluator(new SequentialSolutionListEvaluator<DoubleSolution>())
                .build();
        algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run));
      }

      for (int i = 0; i < problemList.size(); i++) {
        Algorithm<List<DoubleSolution>> algorithm = new NSGAIIBuilder<DoubleSolution>(
                problemList.get(i).getProblem(),
                new SBXCrossover(1.0, 20.0),
                new PolynomialMutation(1.0 / problemList.get(i).getProblem().getNumberOfVariables(),
                        20.0),
                100)
                .build();
        algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run));
      }

      for (int i = 0; i < problemList.size(); i++) {
        Algorithm<List<DoubleSolution>> algorithm = new MOEADBuilder(problemList.get(i).getProblem(), MOEADBuilder.Variant.MOEAD)
                .setCrossover(new DifferentialEvolutionCrossover(1.0, 0.5, DifferentialEvolutionCrossover.DE_VARIANT.RAND_1_BIN))
                .setMutation(new PolynomialMutation(1.0 / problemList.get(i).getProblem().getNumberOfVariables(),
                        20.0))
                .setMaxEvaluations(25000)
                .setPopulationSize(100)
                .setResultPopulationSize(100)
                .setNeighborhoodSelectionProbability(0.9)
                .setMaximumNumberOfReplacedSolutions(2)
                .setNeighborSize(20)
                .setFunctionType(AbstractMOEAD.FunctionType.TCHE)
                .build();

        algorithms.add(new ExperimentAlgorithm<>(algorithm, problemList.get(i), run));
      }
    }
    return algorithms;
  }
}
