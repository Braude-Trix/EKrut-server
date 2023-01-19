package gui;

public interface IServerGui {
	public void setPrintToConsole(String msg, boolean isError);
	public void setPrintToConsole(String msg);
	public void setConnected(boolean isConnected);

}
