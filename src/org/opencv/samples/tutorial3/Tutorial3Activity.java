package org.opencv.samples.tutorial3;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

public class Tutorial3Activity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "OCVSample::Activity";

    private Tutorial3View mOpenCvCameraView;
    private List<Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;
    
	private List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	private Mat hierarchy;
	private Mat mIntermediateMat;
	private MatOfPoint2f approxCurve;
	private Size resolution = null;

	/*OpenCv Variables*/
	private Mat mRgba;
	private Mat mGray;
	
	boolean onCameraViewStarted = true;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(Tutorial3Activity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public Tutorial3Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial3_surface_view);

        mOpenCvCameraView = (Tutorial3View) findViewById(R.id.tutorial3_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
      

        
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView(); 
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    	if(onCameraViewStarted == true){
    		onCameraViewStarted = false;
	        mResolutionList = mOpenCvCameraView.getResolutionList();
	        for(int i=0; i<mResolutionList.size(); i++){
	        	Log.e("mResolutionList", mResolutionList.get(i).height+", "+mResolutionList.get(i).width);
	        	if(mResolutionList.get(i).width == 640){
	        		resolution = mResolutionList.get(i);
	        		mOpenCvCameraView.setResolution(resolution);
	        		resolution = mOpenCvCameraView.getResolution();
	        		String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
	        		Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
	        	}
	        }
        }
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat mRgba = inputFrame.rgba();
		Point resolutionPoint = new Point(inputFrame.rgba().width(), inputFrame.rgba().height());

		// 二值化
		Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGBA2GRAY, 0);

		// 高斯濾波器
		Imgproc.GaussianBlur(mRgba, mRgba, new org.opencv.core.Size(3, 3), 6);

		// 邊緣偵測
		Imgproc.Canny(mRgba, mRgba, 360, 180);

		// 蝕刻
		Imgproc.erode(mRgba, mRgba, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(1, 1)));

		// 膨脹
		Imgproc.dilate(mRgba, mRgba, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(4, 4)));

		contours = new ArrayList<MatOfPoint>();
		hierarchy = new Mat();

		// 找影像輪廓		
		Imgproc.findContours(mRgba, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
		hierarchy.release();
		
		if(contours.size() != 0 &&contours.size() < 500){
			
			// 劃出輪廓線
			Imgproc.drawContours(inputFrame.rgba(), contours, -1, new Scalar(255, 255, 0, 255), 1);       	        
	        
	        //For each contour found
	        approxCurve = new MatOfPoint2f();
	        for (int i=0; i<contours.size(); i++)
	        {
	            //Convert contours(i) from MatOfPoint to MatOfPoint2f
	            MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(i).toArray() );	            
	            
	            //Processing on mMOP2f1 which is in type MatOfPoint2f
	            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
	            
	            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

	            //Convert back to MatOfPoint
	            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

	            // Get bounding rect of contour
	            Rect rect = Imgproc.boundingRect(points);
	            
	            if(i==0){
	            	
	            	// 質心
	            	// http://monkeycoding.com/?p=617
	            	Moments mu = Imgproc.moments(contours.get(i), false);
	            	Point momentsPoint = new Point((int)(mu.get_m10() / mu.get_m00()), (int)(mu.get_m01() / mu.get_m00()));
//	            	Core.circle(mRgba, momentsPoint, 10, new Scalar(255, 255, 0, 255), -1);
			        Core.rectangle(mRgba, new Point(momentsPoint.x-10, momentsPoint.y-10), 
			        		new Point(momentsPoint.x+10, momentsPoint.y+10), new Scalar(0, 255, 255, 255), 2); 
	            	
	            	// 面積
			        // http://monkeycoding.com/?p=617
		            double contourArea = Imgproc.contourArea(contour2f, false);
		            Core.putText(mRgba, String.valueOf(contourArea), 
	            			new Point(10, resolutionPoint.y - 45), 3, 1, new Scalar(0, 255, 128, 255), 2);
		            
		            // 周長
		            // http://monkeycoding.com/?p=617
		            double arcLength = Imgproc.arcLength(contour2f, true);
		            Core.putText(mRgba, String.valueOf(arcLength), 
	            			new Point(10, resolutionPoint.y - 15), 3, 1, new Scalar(0, 255, 128, 255), 2);
		            
//		            // 凸殼
//		            // http://monkeycoding.com/?p=612
//		            MatOfInt mOi= new MatOfInt();
//		            Imgproc.convexHull(contours.get(i), mOi);                    
//                    Point convexHullPoint = contours.get(i).toList().get(mOi.toList().get(i));
//                    Core.circle(mRgba, convexHullPoint, 10, new Scalar(255, 0, 0, 255), -1);

	            }else{
	            	Moments mu = Imgproc.moments(contours.get(i), false);
	            	Point momentsPoint = new Point((int)(mu.get_m10() / mu.get_m00()), (int)(mu.get_m01() / mu.get_m00()));
//	            	Core.circle(mRgba, momentsPoint, 10, new Scalar(255, 255, 0, 255), -1);
			        Core.rectangle(mRgba, new Point(momentsPoint.x-10, momentsPoint.y-10), 
			        		new Point(momentsPoint.x+10, momentsPoint.y+10), new Scalar(0, 255, 0, 255), 2); 
	            }
	            
//	            Point centerPoint = new Point(rect.x+(rect.width)/2, rect.y+(rect.height)/2);
//		        Core.rectangle(mRgba, new Point(centerPoint.x-10, centerPoint.y-10), 
//		        		new Point(centerPoint.x+10, centerPoint.y+10), new Scalar(0, 255, 0, 255), 2); 
	            
	            // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
//	            Core.rectangle(mRgba, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height), new Scalar(0, 255, 0, 255), 2); 
	        }
	        
//			List<Moments> mu = new ArrayList<Moments>(contours.size());
//		    for (int i = 0; i < contours.size(); i++) {
//		        mu.add(i, Imgproc.moments(contours.get(i), false));
//		        Moments pMoments = mu.get(i);
//		        int xMoments = (int) (pMoments.get_m10() / pMoments.get_m00());
//		        int yMoments = (int) (pMoments.get_m01() / pMoments.get_m00());
//		        Log.e("sizeRgba", "("+xMoments+", "+yMoments+")");
//		        Core.putText(mRgba, String.valueOf(i+1), new Point(xMoments, yMoments), 1, 1, new Scalar(255, 127, 0, 255), 2);
//		        Core.rectangle(mRgba, new Point(xMoments-10, yMoments-10), new Point(xMoments+10, yMoments+10), new Scalar(0, 255, 0, 255), 2); 
//		    };

			
//			for(int i=0; i<contours.size(); i++){				
//				int rows = (int) contours.get(i).size().height + 20;
//		        int cols = (int) contours.get(i).size().width + 60;       
//		        Log.e("sizeRgba", "("+rows+", "+cols+")");
//		        Core.putText(mRgba, String.valueOf(i+1), new Point(rows, cols), 1, 1, new Scalar(255, 255, 0, 255), 2);
//			}
			
		}else{
			Core.trace(inputFrame.rgba());
		}
		Core.putText(mRgba, String.valueOf(contours.size()), new Point(10, resolutionPoint.y - 75), 3, 1, new Scalar(255, 0, 0, 255), 2);		
		return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        List<String> effects = mOpenCvCameraView.getEffectList();

        if (effects == null) {
            Log.e(TAG, "Color effects are not supported by device!");
            return true;
        }

//        mColorEffectsMenu = menu.addSubMenu("Color Effect");
//        mEffectMenuItems = new MenuItem[effects.size()];
        
        int idx = 0;
//        ListIterator<String> effectItr = effects.listIterator();
//        while(effectItr.hasNext()) {
//           String element = effectItr.next();
//           mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
//           idx++;
//        }

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
        idx = 0;
        while(resolutionItr.hasNext()) {
            Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
         }
        
//        mColorEffectsMenu = menu.addSubMenu("Color Effect");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        
//        Toast.makeText(this, String.valueOf(item.getItemId()), Toast.LENGTH_SHORT).show();
        
        if (item.getGroupId() == 1)
        {
            mOpenCvCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2)
        {	
        	
            int id = item.getItemId();
            Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG,"onTouch event");
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
//        String currentDateandTime = sdf.format(new Date());
//        String fileName = Environment.getExternalStorageDirectory().getPath() +
//                               "/sample_picture_" + currentDateandTime + ".jpg";
//        mOpenCvCameraView.takePicture(fileName);
//        Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
        return false;
    }
}
