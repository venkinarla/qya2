package Server;

import State.Coordinate;

public interface MapControl
{
	abstract public void OnStartMonitor();
	abstract public void OnEndMonitor();
	abstract public void OffStartMonitor();
	abstract public void OffEndMonitor();
	abstract public void setMovePath(Coordinate Start, Coordinate End);
	abstract public void setMove(boolean flag);
}