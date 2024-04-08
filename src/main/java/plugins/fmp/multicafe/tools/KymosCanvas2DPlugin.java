package plugins.fmp.multicafe.tools;

import icy.canvas.IcyCanvas;
import icy.gui.viewer.Viewer;
import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginCanvas;


public class KymosCanvas2DPlugin extends Plugin implements PluginCanvas
{
	@Override
	public String getCanvasClassName() {
//		return KymosCanvas2DPlugin.class.getName();
		return "KymosView";
	}

	@Override
	public IcyCanvas createCanvas(Viewer viewer) 
	{		
		return new KymosCanvas2D(viewer);
	}

}
