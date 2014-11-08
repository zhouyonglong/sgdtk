package org.sgdtk.exec;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import org.sgdtk.*;
import org.sgdtk.struct.SGDLearner;
import org.sgdtk.fileio.SVMLightFileFeatureProvider;
import org.sgdtk.SquareLoss;
import java.io.*;
import java.util.List;

/**
 * Train a classifier using some loss function using SGD
 *
 * @author dpressel
 */
public class Train
{

    public static class Params
    {

        @Parameter(description = "Training file", names = {"--train", "-t"}, required = true)
        public String train;

    	@Parameter(description = "Testing file", names = {"--eval", "-e"})
        public String eval;

        @Parameter(description = "Model to write out", names = {"--model", "-s"})
        public String model;

        @Parameter(description = "Loss function", names = {"--loss", "-l"})
        public String loss = "hinge";

        @Parameter(description = "lambda", names = {"--lambda", "-lambda"})
        public Double lambda = 1e-5;

        @Parameter(description = "Number of epochs", names = {"--epochs", "-epochs"})
        public Integer epochs = 5;

        @Parameter(description = "Number of examples", names = {"--ntex", "-x"})
        public Integer numTrainExamples;

        @Parameter(description = "Width of feature vector", names = {"--wfv", "w"})
        public Integer widthFV;

	}

	public static void main(String[] args)
    {
        try
        {
            Params params = new Params();
            JCommander jc = new JCommander(params, args);
            jc.parse();

            File trainFile = new File(params.train);
            SVMLightFileFeatureProvider.Dims dims;
            if (params.widthFV == null)
            {
                dims = SVMLightFileFeatureProvider.findDims(trainFile);
                System.out.println("Dims: " + dims.width + " x " + dims.height);
            }
            else
            {
                dims = new SVMLightFileFeatureProvider.Dims(params.widthFV, 0);
            }
            SVMLightFileFeatureProvider reader = new SVMLightFileFeatureProvider(dims.width);

            List<FeatureVector> trainingSet = reader.load(trainFile);

            List<FeatureVector> evalSet = null;
            if (params.eval != null)
            {
                File evalFile = new File(params.eval);
                evalSet = reader.load(evalFile);
            }

            Loss lossFunction = null;
            if (params.loss.equalsIgnoreCase("log"))
            {
                System.out.println("Using log loss");
                lossFunction = new LogLoss();
            }
            else if (params.loss.startsWith("sq"))
            {
                System.out.println("Using square loss");
                lossFunction = new SquareLoss();
            }
            else
            {
                System.out.println("Using hinge loss");
                lossFunction = new HingeLoss();
            }
            Learner learner = new SGDLearner(lossFunction, params.lambda);

            FeatureVector fv0 = trainingSet.get(0);
            Model model = learner.create(fv0.length());
            for (int i = 0; i < params.epochs; ++i)
            {
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                System.out.println("EPOCH: " + (i + 1));
                Metrics metrics = new Metrics();
                double t0 = System.currentTimeMillis();

                learner.trainEpoch(model, trainingSet);
                double tn = System.currentTimeMillis();
                System.out.println("Total training time " + (tn-t0)/1000. + "s");


                learner.eval(model, trainingSet, metrics);
                showMetrics(metrics, "Training Set Eval Metrics");
                metrics.clear();

                if (evalSet != null)
                {
                    learner.eval(model, evalSet, metrics);
                    showMetrics(metrics, "Test Set Eval Metrics");
                }
            }

            if (params.model != null)
            {
                model.save(new FileOutputStream(params.model));

            }
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
	}

    private static void showMetrics(Metrics metrics, String pre)
    {
        System.out.println("========================================================");
        System.out.println(pre);
        System.out.println("========================================================");

        System.out.println("\tLoss = " + metrics.getLoss());
        System.out.println("\tCost = " + metrics.getCost());
        System.out.println("\tError = " + 100*metrics.getError());
        System.out.println("--------------------------------------------------------");
    }


}