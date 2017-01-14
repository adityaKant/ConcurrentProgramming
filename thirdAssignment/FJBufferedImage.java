package cop5618;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;


public class FJBufferedImage extends BufferedImage {

	private class GetRGBTask extends RecursiveAction{

		int startX;
		int startY;
		int w;
		int h;
		int[] rgbArray;
		int offset;
		int scansize;
		FJBufferedImage image;

		public GetRGBTask(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize, FJBufferedImage image){
			this.startX = startX;
			this.startY = startY;
			this.w = w;
			this.h = h;
			this.rgbArray = rgbArray;
			this.offset = offset;
			this.scansize = scansize;
			this.image = image;
		}

		protected void compute(){
			if(this.h <= 400)
				image.callGetRGB(startX, startY, w, h, rgbArray, w*startY, w);
			else{
				GetRGBTask upperHalf;
				GetRGBTask lowerHalf;


				if(h % 2 == 0){
					upperHalf = new GetRGBTask(startX, startY, w, h/2,rgbArray, scansize * startY, scansize, image);
					lowerHalf = new GetRGBTask(startX, startY + h/2, w, h/2, rgbArray, scansize * (startY+h/2), scansize, image);

				}
				else{
					upperHalf = new GetRGBTask(startX, startY, w, h/2, rgbArray, scansize * startY, scansize, image);
					lowerHalf = new GetRGBTask(startX, startY + h/2, w, h/2+1, rgbArray, scansize * (startY+h/2), scansize, image);

				}

				invokeAll(upperHalf,lowerHalf);
			}
		}
	}

	private class SetRGBTask extends RecursiveAction{

		int startX;
		int startY;
		int w;
		int h;
		int[] rgbArray;
		int offset;
		int scansize;
		FJBufferedImage image;

		public SetRGBTask(int startX, int startY, int w, int h,int[] rgbArray, int offset, int scansize, FJBufferedImage image){
			this.startX = startX;
			this.startY = startY;
			this.w = w;
			this.h = h;
			this.rgbArray = rgbArray;
			this.offset = offset;
			this.scansize = scansize;
			this.image = image;
		}

		protected void compute(){
			if(h <= 400)
				image.callSetRGB(startX, startY, w, h, rgbArray, w*startY, w);
			else{
				SetRGBTask upperHalf;
				SetRGBTask lowerHalf;

				if(h % 2 == 0){
					upperHalf = new SetRGBTask(startX, startY, w, h/2, rgbArray, scansize*startY, scansize, image);
					lowerHalf = new SetRGBTask(startX, startY + h/2, w, h/2, rgbArray, scansize*(startY + h/2), scansize, image);
				}
				else{
					upperHalf = new SetRGBTask(startX, startY, w, h/2, rgbArray, scansize*startY, scansize, image);
					lowerHalf = new SetRGBTask(startX, startY + (h/2), w, h/2+1, rgbArray, scansize*(startY + (h/2)), scansize, image);
				}

				invokeAll(upperHalf,lowerHalf);

			}
		}
	}

	private int[] callGetRGB(int startX,int startY,int w,int h,int[] rgbArray,int offset,int scansize){
		return super.getRGB(startX,startY,w,h,rgbArray,offset,scansize);
	}

	private void callSetRGB(int startX,int startY,int w,int h,int[] rgbArray,int offset,int scansize){
		super.setRGB(startX,startY,w,h,rgbArray,offset,scansize);
	}
	
   /**Constructors*/
	
	public FJBufferedImage(int width, int height, int imageType) {
		super(width, height, imageType);
	}

	public FJBufferedImage(int width, int height, int imageType, IndexColorModel cm) {
		super(width, height, imageType, cm);
	}

	public FJBufferedImage(ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied,
			Hashtable<?, ?> properties) {
		super(cm, raster, isRasterPremultiplied, properties);
	}
	

	/**
	 * Creates a new FJBufferedImage with the same fields as source.
	 * @param source
	 * @return
	 */
	public static FJBufferedImage BufferedImageToFJBufferedImage(BufferedImage source){
	       Hashtable<String,Object> properties=null; 
	       String[] propertyNames = source.getPropertyNames();
	       if (propertyNames != null) {
	    	   properties = new Hashtable<String,Object>();
	    	   for (String name: propertyNames){properties.put(name, source.getProperty(name));}
	    	   }
	 	   return new FJBufferedImage(source.getColorModel(), source.getRaster(), source.isAlphaPremultiplied(), properties);		
	}
	
	@Override
	public void setRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize){
        /****IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER*****/
		ForkJoinPool setPool = new ForkJoinPool();
		setPool.invoke(new SetRGBTask(xStart,yStart,w,h,rgbArray,offset,scansize,this));
	}
	

	@Override
	public int[] getRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize){
	       /****IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER*****/
		ForkJoinPool getPool = new ForkJoinPool();
		getPool.invoke(new GetRGBTask(xStart,yStart,w,h,rgbArray,offset,scansize,this));
		return rgbArray;

	}
	

}

