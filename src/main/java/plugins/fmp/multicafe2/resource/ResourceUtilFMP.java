package plugins.fmp.multicafe2.resource;

import java.awt.Image;
import java.io.InputStream;

import icy.image.ImageUtil;
import icy.resource.ResourceUtil;
import icy.resource.icon.IcyIcon;
import plugins.fmp.multicafe2.MultiCAFE2;



// adapted from NherveToolbox

public class ResourceUtilFMP {

    public static final String ALPHA_PATH 		= "alpha/";
    public static final String ICON_PATH 		= "icon/";
//    public static final String RESOURCES_PATH   = "resources/";
//    public static final String ROOT_PATH 		= "../../../";
    
    public static final IcyIcon ICON_PREVIOUS_IMAGE 	= getIcyIcon("br_prev.png");
    public static final IcyIcon ICON_NEXT_IMAGE  		= getIcyIcon("br_next.png");
    public static final IcyIcon ICON_FIT_YAXIS  		= getIcyIcon("fit_Y.png");
    public static final IcyIcon ICON_FIT_XAXIS  		= getIcyIcon("fit_X.png");
//    public static final IcyIcon ALT_ICON_FIT_YAXIS  	= getIcyIcon("cursor_H_split.png");
//    public static final IcyIcon ALT_ICON_FIT_XAXIS  	= getIcyIcon("cursor_V_split.png");
    

  
    public static IcyIcon getIcyIcon(String fileName) 
    {
		return new IcyIcon(getImage(fileName));
	}
    
	private static Image getImage(String fileName) 
	{
		Image img = ResourceUtil.getAlphaIconAsImage(fileName);
		if (img != null)
			return img;
		
		String name = ICON_PATH + ALPHA_PATH + fileName;
		InputStream url = MultiCAFE2.class.getClassLoader().getResourceAsStream(name);
		if (url == null) {
			System.out.println("ResourceUtilFMP:getImage resource not found: at: "+ name);
		}
		return ImageUtil.load(url);
	}
	
}
