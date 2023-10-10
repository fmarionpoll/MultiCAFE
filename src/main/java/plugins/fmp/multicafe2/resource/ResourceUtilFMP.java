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
    public static final String RESOURCES_PATH   = "resources/";
    public static final String ROOT_PATH 		= "../../../";
    
    public static final IcyIcon ICON_FIT_YAXIS  = getIcyIcon("fit_y.png");
    public static final IcyIcon ICON_FIT_XAXIS  = getIcyIcon("fit_x.png");
    
    
    private static IcyIcon getIcyIcon(String fileName) 
    {
		return new IcyIcon(getImage(fileName));
	}
    
	private static Image getImage(String fileName) 
	{
//		String pkg = ClassUtil.getPackageName(MultiCAFE2.class.getName()) + ".";
//		pkg = ClassUtil.getPathFromQualifiedName(pkg);
//		pkg += RESOURCES_PATH+ fileName;
//		System.out.println(pkg);
		
		String name = ICON_PATH + ALPHA_ICON_PATH + fileName;
		InputStream url = MultiCAFE2.class.getClassLoader().getResourceAsStream(name);
		if (url == null) {
			System.out.println("1 - resource not found: at: "+ name);
			String name1 = RESOURCES_PATH + name;
			url = MultiCAFE2.class.getClassLoader().getResourceAsStream(name1);
			if (url == null) 
			{			
				System.out.println("2 - resource not found either at: "+ name1);
				String name2 = ROOT_PATH + name;
				url = MultiCAFE2.class.getClassLoader().getResourceAsStream(name2);
				if (url == null) 
					System.out.println("3 - resource not found either at: "+ name2);
			}
		}
		return ImageUtil.load(url);
	}
	
}
