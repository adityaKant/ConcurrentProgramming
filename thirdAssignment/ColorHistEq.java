package cop5618;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ColorHistEq {

	//Use these labels to instantiate you timers.  You will need 8 invocations of now()
	static String[] labels = { "getRGB", "convert to HSB", "create brightness map", "probability array",
			"parallel prefix", "equalize pixels", "setRGB" };
	static int binSize = 256;

	static Timer colorHistEq_serial(BufferedImage image, BufferedImage newImage) {
		Timer times = new Timer(labels);

		/**
		 * IMPLEMENT SERIAL METHOD
		 */

		ColorModel colorModel = ColorModel.getRGBdefault();
		int w = image.getWidth();
		int h = image.getHeight();
		times.now();


		int[] sourcePixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
		times.now();

		float[][] hsbArr = new float[sourcePixelArray.length][3];
		IntStream.range(0,sourcePixelArray.length)
				.forEach(i -> {
					Color.RGBtoHSB(colorModel.getRed(sourcePixelArray[i]), colorModel.getGreen(sourcePixelArray[i]),
							colorModel.getBlue(sourcePixelArray[i]), hsbArr[i]);
				});
		times.now();


		Map<Integer,Integer> brightnessMap = Arrays.stream(hsbArr)
				.mapToInt(HSBComp -> (int) (HSBComp[2]*binSize))
				.mapToObj(Integer::new)
				.collect(Collectors.groupingBy(Function.identity(),Collectors.summingInt(count -> 1)));

		int []binArr =  brightnessMap.keySet().stream().mapToInt(i -> brightnessMap.get(i)).toArray();
		times.now();


		Arrays.parallelPrefix(binArr,( x, y ) -> x + y);
		times.now();


		double []cummProbability = Arrays.stream(binArr).mapToDouble(x -> x/(double)binArr[binArr.length-1]).toArray();
		times.now();


		float[][] equalizationArr = Arrays.stream(hsbArr)
				.map(i -> {
					i[2] = (float) cummProbability[(int)(i[2] * (binSize-1))];
					return i;
				})
				.toArray(float[][]::new);
		times.now();


		int[] destinationPixArr = Arrays.stream(equalizationArr).mapToInt(hsbComp-> Color.HSBtoRGB(hsbComp[0],hsbComp[1],hsbComp[2])).toArray();
		newImage.setRGB(0,0,w,h,destinationPixArr,0,w);
		times.now();

		return times;
	}



	static Timer colorHistEq_parallel(FJBufferedImage image, FJBufferedImage newImage) {
		Timer times = new Timer(labels);

		/**
		 * IMPLEMENT PARALLEL METHOD
		 */


		ColorModel colorModel = ColorModel.getRGBdefault();
		int w = image.getWidth();
		int h = image.getHeight();
		times.now();


		int[] sourcePixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
		times.now();

		float[][] hsbArr = new float[sourcePixelArray.length][3];
		IntStream.range(0,sourcePixelArray.length)
				.parallel()
				.forEach(i -> {
					Color.RGBtoHSB(colorModel.getRed(sourcePixelArray[i]), colorModel.getGreen(sourcePixelArray[i]),
							colorModel.getBlue(sourcePixelArray[i]), hsbArr[i]);
				});
		times.now();


		Map<Integer,Integer> brightnessMap = Arrays.stream(hsbArr)
				.parallel()
				.mapToInt(HSBComp -> (int) (HSBComp[2]*binSize))
				.mapToObj(Integer::new)
				.collect(Collectors.groupingBy(Function.identity(),Collectors.summingInt(count -> 1)));

		int []binArr = brightnessMap.keySet().stream().parallel().mapToInt(i -> brightnessMap.get(i)).toArray();
		times.now();


		Arrays.parallelPrefix(binArr,( x, y ) -> x + y);
		times.now();


		double []cummProbability = Arrays.stream(binArr).parallel().mapToDouble(x -> x/(double)binArr[binArr.length-1]).toArray();
		times.now();


		float[][] equalizationArr = Arrays.stream(hsbArr)
				.parallel()
				.map(i -> {
					i[2] = (float) cummProbability[(int)(i[2] * (binSize-1))];
					return i;
				})
				.toArray(float[][]::new);
		times.now();


		int[] destinationPixArr = Arrays.stream(equalizationArr).parallel().mapToInt(hsbComp-> Color.HSBtoRGB(hsbComp[0],hsbComp[1],hsbComp[2])).toArray();
		newImage.setRGB(0,0,w,h,destinationPixArr,0,w);
		times.now();

		return times;
	}

}