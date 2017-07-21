/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.acme.snap.examples.data_export;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductSubsetBuilder;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.jexp.ParseException;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This is an example program which writes out a bit-mask image of an ENVISAT data product. The image is written as a
 * raw byte stream and contains as much bytes as a the number of pixels a scene of the product has.
 * <p>The program expects three input arguments: <ol> <li><i>input-file</i> - the input file path to an ENVISAT data
 * product</li> <li><i>output-file</i> - the file path to the bit-mask image file to be written</li>
 * <li><i>bit-mask-expr</i> - the bit-mask expression</li> </ol>
 * <p><i>bit-mask-expr</i> is a boolean expression. The logical operators you can use are "AND", "OR" and "NOT" (or the
 * characters "&amp;", "&#124;" and "!" respectively). You can also enclose sub-expressions in parentheses "(" and ")" to change
 * the evaluation priority.
 * <p>A reference to a flag value in a data product comprises the flag dataset name followed by a dot "." and followed
 * by the flag name you are interested in. For example, "l1_flags.INVALID" references the INVALID-flag in the dataset
 * "l1_flags".
 * <p>The following are examples for valid bit-mask expression strings for MERIS L1b products: <ul> <li><code>"NOT
 * l1_flags.INVALID AND NOT l1_flags.BRIGHT"</code></li> <li><code>"(l1_flags.COASTLINE OR l1_flags.LAND_OCEAN) AND NOT
 * l1_flags.GLINT_RISK"</code></li> <li><code>"!(l1_flags.BRIGHT | l1_flags.GLINT_RISK | l1_flags.INVALID |
 * l1_flags.SUSPECT)"</code></li> </ul>
 * <p>The following are examples for valid bit-mask expression strings for AATSR TOA L1b products: <ul> <li><code>"NOT
 * confid_flags_nadir.SATURATION AND NOT confid_flags_nadir.OUT_OF_RANGE"</code> <li><code>"confid_flags_nadir.SATURATION
 * OR confid_flags_fward.SATURATION"</code> <li><code>"cloud_flags_nadir.LAND OR cloud_flags_fward.SUN_GLINT"</code>
 * <li><code>"cloud_flags_nadir.CLOUDY and not confid_flags_nadir.NO_SIGNAL"</code> <li><code>"cloud_flags_fward.CLOUDY
 * and not confid_flags_fward.NO_SIGNAL"</code> </ul>
 * <p>You find all possible flag datasets in the <code><b>beam.jar</b>!/org/esa/snap/resources/dddb/bands/<i>product-type</i>.dd</code>
 * file, where <i>product-type</i> is an ENVISAT product type name, such as "MER_RR__1P" for a MERIS L1b reduced
 * resolution product. The corresponding flags you can find in a dataset are stored under
 * <code><b>beam.jar</b>!/org/esa/snap/resources/dddb/bands/<i>product-type</i>_<i>dataset_type</i>.dd</code>.
 * <i><b>Note:</b> If you want to work with product subsets, you can use the {@link
 * ProductSubsetBuilder} class. It has a static method which lets you create a subset of a
 * given product and subset definition.</i>
 *
 * @see ProductIO
 * @see ProductSubsetBuilder
 * @see ProductSubsetDef
 * @see Product
 * @see Band
 * @see TiePointGrid
 */
public class MaskWriterMain {

    /**
     * The main method. Fetches the input arguments and delegates the call to the <code>run</code> method.
     *
     * @param args the program arguments
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("parameter usage: <input-file> <output-file> <mask-expr>");
            return;
        }
        // Get arguments
        String inputPath = args[0];
        String outputPath = args[1];
        String maskExpr = args[2];
        try {
            // Pass arguments to actual program code
            run(inputPath, outputPath, maskExpr);
        } catch (IOException e) {
            System.out.println("I/O error: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("bit-mask syntax error: " + e.getMessage());
        }
    }

    /**
     * Runs this program with the given parameters.
     */
    private static void run(String inputPath, String outputPath, String maskExpr)
            throws IOException,
                   ParseException {

        // Read the product (note that only 'nodes' are read, not the entire data!)
        Product product = ProductIO.readProduct(inputPath);

        // Get the scene width
        int w = product.getSceneRasterWidth();
        // Get the scene height
        int h = product.getSceneRasterHeight();

        // Print out, what we are going to do...
        System.out.println("writing mask image file "
                           + outputPath
                           + " containing " + w + " x " + h + " pixels of type byte...");

        // Open output stream for our mask image
        FileOutputStream outputStream = new FileOutputStream(outputPath);

        // Create the mask buffer for a single scan line
        int[] maskScanLine = new int[w];
        byte[] byteScanLine = new byte[w];

        RenderedImage maskImage = product.getMaskImage(maskExpr, null).getImage(0);
        // For all scan lines in the product...
        for (int y = 0; y < h; y++) {
            // Read the bit-mask scan line at y
            maskImage.getData(new Rectangle(0, y, w, 1)).getSamples(0, y, w, 1, 0, maskScanLine);
            for (int i = 0; i < maskScanLine.length; i++) {
                byteScanLine[i] = (byte) maskScanLine[i];
            }
            // write bit-mask scan line to image file
            outputStream.write(byteScanLine);
        }

        // close bit-mask image file
        outputStream.close();

        // Done!
        System.out.println("OK");
    }
}
