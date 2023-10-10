package plugins.fmp.multicafe2.resource;

import java.awt.Image;
import java.io.InputStream;

import icy.image.ImageUtil;
import icy.resource.icon.IcyIcon;
import plugins.fmp.multicafe2.MultiCAFE2;




// adapted from NherveToolbox


public class ResourceUtilFMP {

    public static final String ALPHA_ICON_PATH 	= "alpha/";
    public static final String ICON_PATH 		= "icon/";
    public static final String RESOURCES_PATH 	= "resources/";
    
    public static final IcyIcon ICON_FIT_YAXIS  = getIcyIcon("fit_y.png");
    public static final IcyIcon ICON_FIT_XAXIS  = getIcyIcon("fit_x.png");
    
    
    private static IcyIcon getIcyIcon(String fileName) {
    	String name = 
    			ICON_PATH + 
    			ALPHA_ICON_PATH + 
    			fileName;
		return new IcyIcon(getImage(name));
	}
    
	private static Image getImage(String fileName) {
//		String pkg = ClassUtil.getPackageName(MultiCAFE2.class.getName()) + ".";
//		pkg = ClassUtil.getPathFromQualifiedName(pkg);
//		pkg += RESOURCES_PATH+ fileName;
//		System.out.println(pkg);
		
		InputStream url = MultiCAFE2.class.getClassLoader().getResourceAsStream(fileName);
		return ImageUtil.load(url);
	}
}
