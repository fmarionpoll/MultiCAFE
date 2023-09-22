package plugins.fmp.multicafe2.tools;

import icy.canvas.IcyCanvas;
import icy.gui.viewer.Viewer;
import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginCanvas;


public class KymosCanvas2DPlugin extends Plugin implements PluginCanvas
{
	@Override
	public String getCanvasClassName() {
		return KymosCanvas2DPlugin.class.getName();
	}

	@Override
	public IcyCanvas createCanvas(Viewer viewer) 
	{		
		return new KymosCanvas2D(viewer);
	}

}
