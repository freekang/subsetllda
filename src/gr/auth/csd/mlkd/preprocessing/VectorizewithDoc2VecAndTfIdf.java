/*
 * Copyright (C) 2015 Yannis Papanikolaou <ypapanik@csd.auth.gr>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package gr.auth.csd.mlkd.atypon.preprocessing;

/**
 *
 * @author Yannis Papanikolaou <ypapanik@csd.auth.gr>
 */
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.nd4j.linalg.api.ndarray.INDArray;

public class VectorizewithDoc2VecAndTfIdf extends VectorizewithDoc2Vec {

    public VectorizewithDoc2VecAndTfIdf(Dictionary d, Labels l, Corpus corpus) {
        super(d, l, corpus);
    }

    @Override
    protected void vectorizeLabeled(Corpus corpus, String libsvmFilename, String labelsFile, boolean perLabel, String metaTrainFileName) {

        corpus.reset();
        try (BufferedWriter output = Files.newBufferedWriter(Paths.get(libsvmFilename), Charset.forName("UTF-8"));
                ObjectOutputStream outLabels = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(labelsFile)))) {
            //System.out.println(new Date() + " Vectorizing labeled data...");

            TIntObjectHashMap<TreeSet<Integer>> labelValues = new TIntObjectHashMap<>();
            TIntArrayList targetValues = new TIntArrayList();
            ArrayList<TIntList> targetValuesPerDoc = new ArrayList<>();

            // read each file in given directory and parse the text as follows
            List<String> lines;
            Document doc;

            int counter = 0;
            while ((doc = corpus.nextDocument()) != null) {
                counter++;

                // output features in shell libsvm file
                StringBuilder sb = new StringBuilder();
                sb.append("0"); //the label value
                //features
                lines = doc.getContentAsSentencesOfTokens(false);

                StringBuilder sb0 = new StringBuilder();
                for (String line : lines) {
                    sb0.append(line).append(" ");
                }
                INDArray inferVector = pvm.doc2vec(sb0.toString(), doc.getLabels());
                Map<Integer, Double> vector = new TreeMap<>();
                for (int i = 0; i < inferVector.length(); i++) {
                    double value = inferVector.getDouble(i);
                    if (value != 0) {
                        vector.put(i+1, value);
                    }
                }

//                Map<Integer, Double> vector2 = vectorize(lines, true, doc);
//                Iterator<Map.Entry<Integer, Double>> it = vector2.entrySet().iterator();
//                while(it.hasNext()) {
//                    Map.Entry<Integer, Double> next = it.next();
//                    vector.put(next.getKey()+ 1 + inferVector.length(), next.getValue());
//                }
                // output features in shell libsvm file
                Iterator<Map.Entry<Integer, Double>> values = vector.entrySet().iterator();
                while (values.hasNext()) {
                    Map.Entry<Integer, Double> entry = values.next();
                    if (entry.getValue() != 0) {
                        sb.append(" ");
                        sb.append(entry.getKey()).append(":")
                                .append(String.format(Locale.US, "%.6f", entry.getValue()));
                    }
                }

                sb.append("\n");
                output.write(sb.toString());

                // record labels
                Set<String> meshTerms = doc.getLabels();
                int cardinality = 0;
                TIntList docLabels = new TIntArrayList();
                for (String term : meshTerms) {
                    //System.out.println("line: " + line);
                    Integer x = labels.getIndex(term);
                    if (x == -1) { //CHANGE in week 5
                        //System.out.println("Label " + line + " not in training corpus");                                
                    } else {
                        docLabels.add(x);
                        cardinality++;
                        TreeSet<Integer> sortedSet;
                        if (labelValues.contains(x)) {
                            sortedSet = labelValues.get(x);
                        } else {
                            sortedSet = new TreeSet<>();
                        }
                        sortedSet.add(counter - 1);
                        labelValues.put(x, sortedSet);
                    }
                }
                //System.out.println("docLabels:" + Arrays.toString(docLabels.toArray()));
                targetValuesPerDoc.add(docLabels);
                targetValues.add(cardinality);
            }

            if (perLabel) {
                outLabels.writeObject(labelValues);
                try (ObjectOutputStream metaTrain = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(metaTrainFileName)))) {
                    metaTrain.writeObject(targetValues);
                }
            } else {
                outLabels.writeObject(targetValuesPerDoc);
            }

        } catch (IOException ex) {
            Logger.getLogger(VectorizewithDoc2VecAndTfIdf.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void vectorizeUnlabeled(Corpus aCorpus, String libsvmFilename) {
        try (BufferedWriter output = Files.newBufferedWriter(Paths.get(libsvmFilename), Charset.forName("UTF-8"))) {
            // read each file in given directory and parse the text as follows
            List<String> lines;
            Document doc;
            int counter = 0;
            aCorpus.reset();
            while ((doc = aCorpus.nextDocument()) != null) {
                counter++;
                // output features in shell libsvm file
                StringBuilder sb = new StringBuilder();
                sb.append("0"); //the label value
                lines = doc.getContentAsSentencesOfTokens(false);
                StringBuilder sb0 = new StringBuilder();
                for (String line : lines) {
                    sb0.append(line).append(" ");
                }
                INDArray inferVector = pvm.doc2vec(sb0.toString(), doc.getLabels());
                Map<Integer, Double> vector = new TreeMap<>();
                for (int i = 0; i < inferVector.length(); i++) {
                    double value = inferVector.getDouble(i);
                    if (value != 0) {
                        vector.put(i+1, value);
                    }
                }
//                Map<Integer, Double> vector2 = vectorize(lines, true, doc);
//                Iterator<Map.Entry<Integer, Double>> it = vector2.entrySet().iterator();
//                while(it.hasNext()) {
//                    Map.Entry<Integer, Double> next = it.next();
//                    vector.put(next.getKey()+ 1 + inferVector.length(), next.getValue());
//                }
                // output features in shell libsvm file
                Iterator<Map.Entry<Integer, Double>> values = vector.entrySet().iterator();
                while (values.hasNext()) {
                    Map.Entry<Integer, Double> entry = values.next();
                    if (entry.getValue() != 0) {
                        sb.append(" ");
                        sb.append(entry.getKey()).append(":")
                                .append(String.format(Locale.US, "%.6f", entry.getValue()));
                    }
                }
                sb.append("\n");
                output.write(sb.toString());
            }

        } catch (IOException ex) {
            Logger.getLogger(VectorizewithDoc2VecAndTfIdf.class.getName()).log(Level.SEVERE, null, ex);
        }
        //System.out.println(new Date() + " Finished vectorizing unlabeled data.");
    }

        protected Map<Integer, Double> normalizeVector2(Map<Integer, Double> vector) {
        Collection<Double> weights = vector.values();
        Double min = Collections.min(weights);
        if(min<0) {
            Iterator<Map.Entry<Integer, Double>> it = vector.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<Integer, Double> next = it.next();
                next.setValue(next.getValue()-min);
            }
        }
        double length = 0;
        for (Double d : weights) {
            length += d * d;
        }
        length = Math.sqrt(length);
        if (length == 0) {
            length = 1;
        }
        Iterator<Map.Entry<Integer, Double>> values = vector.entrySet().iterator();
        while (values.hasNext()) {
            Map.Entry<Integer, Double> entry = values.next();
            entry.setValue(entry.getValue() / length);

        }
        return vector;
    }
    
}