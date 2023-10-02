package plugins.fmp.multicafe2.resource;

import java.awt.Image;
import java.io.File;
import java.net.URL;

import icy.image.ImageUtil;
import icy.resource.ResourceUtil;
import icy.system.thread.ThreadUtil;

// copied from ResourceUtil.java of Icy source code
// 02-october-2023
// author: Stéphane Dallongeville


public class ResourceUtilFMP {

    public static final String ALPHA_ICON_PATH = "alpha/";
    public static final String ICON_PATH = "icon/";
    
    /**
     * Return an image located in resources/icon/alpha from its name<br>
     */
    public static Image getAlphaIconAsImage(String resourceName)
    {
        return getAlphaIconAsImage(resourceName, -1);
    }
    
    /**
     * Return an image located in resources/icon/alpha with specified square size from its name<br>
     */
    public static Image getAlphaIconAsImage(String resourceName, int size)
    {
        final String finalName;

        if (resourceName.toLowerCase().endsWith(".png"))
            finalName = resourceName;
        else
            finalName = resourceName + ".png";

        return getIconAsImage(ALPHA_ICON_PATH + finalName, size);
    }
    
    /**
     * Return an image with wanted size located in resources/icon from its name<br>
     * For any other location use the ImageUtil.loadImage() method
     * 
     * @param name
     */
    public static Image getIconAsImage(String name, int size)
    {
        final URL url = ResourceUtil.class.getResource("/" + ICON_PATH + name);

        Image result = null;

        if (url != null)
            result = ImageUtil.load(url, false);
        else
            result = ImageUtil.load(new File(ICON_PATH + name), false);

        // FIXME: we do that as in very rare occasion ImageIO.read(..) throw a NPE (inflater has been closed) 
        // I admit this is an ugly fix but it works eventually.. 
        int retry = 3;
        while ((result == null) && (retry-- > 0))
        {
            ThreadUtil.sleep(1);

            if (url != null)
                result = ImageUtil.load(url, false);
            else
                result = ImageUtil.load(new File(ICON_PATH + name), false);
        }

        if (result == null)
        {
            System.out.println("Resource name can't be found: " + name);
            return null;
        }

        return scaleImage(result, size);
    }
    
    private static Image scaleImage(Image image, int size)
    {
        // resize if needed
        if ((image != null) && (size != -1))
        {
            // be sure image data are ready
            ImageUtil.waitImageReady(image);
            // resize only if image has different size
            if ((image.getWidth(null) != size) || (image.getWidth(null) != size))
                return ImageUtil.scale(image, size, size);
        }

        return image;
    }
}
