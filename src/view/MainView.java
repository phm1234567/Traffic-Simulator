package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import control.Controller;
import control.Observer;
import events.Event;
import exceptions.SimulationError;
import model.map.RoadMap;
import model.objects.Road;
import model.objects.GenericJunction;
import model.objects.Vehicle;



@SuppressWarnings("serial")
public class MainView extends JFrame
							implements Observer{
	
	// BORDER
	public static Border defaultBorder =
			BorderFactory.createLineBorder(Color.black, 2); // COLOR, THICK
	
	// MENU AND TOOL BAR
	private JFileChooser fileChooser;
	private ToolBar toolBar;
	

	// SUPERIOR PANEL
	//---------------------------------------------------------------------------
	
	static private final String[] columnIdEvents = { "#", "Time", "Type" };
	
	// MENU
	MenuBar menuBar;
	
	// panels
	private TextAreaPanel eventsEditorPanel;
	private TextAreaPanel reportsPanel;
	private TablePanel<Event> eventsQueuePanel;
	
	// CENTRAL PANEL
	//---------------------------------------------------------------------------
	
	static private final String[] columnIdVehicles = { "ID", "Road",
	 "Location", "Speed", "Km", "Fault Time", "Itinerary" };
	
	static private final String[] columnIdRoads = { "ID", "Origin",
	 "Destination", "Length", "Max Speed", "Vehicles" };
	
	static private final String[] columnIdJunctions = { "ID", "Green Light", "Red Light" };
	
	private TablePanel<Vehicle> vehiclesPanel;
	private TablePanel<Road> roadPanel;
	private TablePanel<GenericJunction<?>> junctionPanel;
	
	// REPORTS DIALOG
	private ReportsDialog reportsDialog; 
	
	// GRAPHICAL COMPONENT
	private MapComponent mapComponent;
	
	// STATUS BAR
	//---------------------------------------------------------------------------
	private StatusBarPanel statusBarPanel;

	
	// MODEL PART - VIEW CONTROLLER MODEL
	//---------------------------------------------------------------------------
	private File currentFile; 
	private Controller controller;
	
	
	// THREAD
	//----------------------------------------------------------------------------
	private Thread t = null; 
	
	
	
	// CONSTRUCTOR
	//-----------------------------------------------------------------------------
	public MainView(String inputFile, Controller ctrl)
			throws FileNotFoundException {

		// Titulo de la ventana
		super("[=] TRAFFIC SIMULATOR [=]");
		controller = ctrl;
		currentFile = inputFile != null ? new File(inputFile) : null;
		
		ctrl.addObserver(this);
		initGUI();
		
		
		if(currentFile != null) {
			String s = readFile(currentFile);
			eventsEditorPanel.setTexto(s);
		}
	}

	// INITIALIZES GRAPHICAL USER INTERFACE
	//------------------------------------------------------------------------------------
	private void initGUI() {
		
		// PANEL PRINCIPAL
		JPanel principalPanel = createPrinciaplPanel(); // border layout
		this.add(principalPanel);
		
		// NORTH
		//---------------------------------------------------------------------------------
		
		createMenu();
		addToolBar(principalPanel);

		
		// CENTER
		//---------------------------------------------------------------------------------
		
		JPanel centralPanel = createCentralPanel();
		principalPanel.add(centralPanel,BorderLayout.CENTER); 
				
	
		createSuperiorCentralPanel(centralPanel);
		createInferiorCentralPanel(centralPanel);
		
		
		// SOUTH
		//---------------------------------------------------------------------------------
		
		addStatusBar(principalPanel);
		
		
		// CLOSE AND VISIBLE
		//----------------------------------------------------------------------------------
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowListener() {
			@Override
			public void windowClosing(WindowEvent e) { exit(); }
			@Override
			public void windowOpened(WindowEvent e) {}
			@Override
			public void windowIconified(WindowEvent e) {}
			@Override
			public void windowDeiconified(WindowEvent e) {}
			@Override
			public void windowDeactivated(WindowEvent e) {}
			@Override
			public void windowClosed(WindowEvent e) {}
			@Override
			public void windowActivated(WindowEvent e) {}
			
		});
		
		// REPORTS DIALOG
		this.reportsDialog = new ReportsDialog(this,this.controller);
		
		this.pack();
		setVisible(true);
	}
	
	// CREATE MENU
	//-----------------------------------------------------------------------------------
	private void createMenu() {
		menuBar = new MenuBar(this,controller);
		this.setJMenuBar(menuBar);
		fileChooser = new JFileChooser(); // File chooser
		fileChooser.setCurrentDirectory(new File("resources/examples/events"));
	}
	
	// CREATE PRINCIPAL PANEL
	//------------------------------------------------------------------------------------
	private JPanel createPrinciaplPanel() {
		
		JPanel principalPanel = new JPanel();
		principalPanel.setLayout(new BorderLayout());

		return principalPanel;
	}
	
	// CREATE CENTRAL PANEL
	//------------------------------------------------------------------------------------
	private JPanel createCentralPanel() {
		
		JPanel centralPanel = new JPanel();
		centralPanel.setLayout(new GridLayout(2,1)); 
		
		return centralPanel;
	}
	
	
	private void createSuperiorCentralPanel(JPanel panelCentral) {
		
		
		JPanel superiorPanel = new JPanel();
		superiorPanel.setLayout(new BoxLayout(superiorPanel, BoxLayout.X_AXIS));
		
		// 1) Events Panel
		String titulo = "Events: ";
		eventsEditorPanel = new EventsEditorPanel(titulo,true,null,this);
		superiorPanel.add(eventsEditorPanel);
		
		// 2) Events Queue Panel
		titulo = "Events Queue: ";
		eventsQueuePanel = new TablePanel<Event>(titulo, 
				new EventsTableModel(MainView.columnIdEvents, controller));
				
		superiorPanel.add(eventsQueuePanel);
		
		// 3) Reports Panel
		titulo = "Reports: ";
		reportsPanel = new ReportsPanel(titulo, false, controller);
		superiorPanel.add(reportsPanel);
		
		// Adds to central panel
		panelCentral.add(superiorPanel);
		
	}
	
	private void createInferiorCentralPanel(JPanel panelCentral) {
		

		JPanel inferiorPanel = new JPanel();
		inferiorPanel.setLayout(new BoxLayout(inferiorPanel, BoxLayout.X_AXIS));
		
		
		JPanel tablesPanel = new JPanel();
		tablesPanel.setLayout(new GridLayout(3,1)); // 3 filas para las tres tablas, 
													// 1(indica sin columnas)
		
		// 1) Vehicles table
		String titulo = "Vehicles";
		vehiclesPanel = new TablePanel<Vehicle>(titulo,
				new VehiclesTableModel(MainView.columnIdVehicles, controller));
		
		// 2) Roads Table
		titulo = "Roads";
		roadPanel = new TablePanel<Road>(titulo,
				new RoadTableModel(MainView.columnIdRoads, controller));
		
		
		// 3) Junction Table
		titulo = "Junctions";
		junctionPanel = new TablePanel<GenericJunction<?>>(titulo,
				new JunctionTableModel(MainView.columnIdJunctions, controller));
		
		
		tablesPanel.add(vehiclesPanel);
		tablesPanel.add(roadPanel);
		tablesPanel.add(junctionPanel);
		
		inferiorPanel.add(tablesPanel);
		
		// graphic component
		mapComponent = new MapComponent(this.controller);
		
		inferiorPanel.add(new JScrollPane(mapComponent,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		
		
		panelCentral.add(inferiorPanel);
		
	}
	
	
	// STATUS BAR
	//------------------------------------------------------------------------------------
	private void addStatusBar(JPanel mainView) {
		statusBarPanel = new StatusBarPanel("Welcome to the simulator !", 
												controller);
		
		mainView.add(statusBarPanel, BorderLayout.PAGE_END);
	}
	
	
	// TOOL BAR
	//-------------------------------------------------------------------------------------
	private void addToolBar(JPanel mainView) {
		
		toolBar = new ToolBar(this, controller);
		mainView.add(toolBar, BorderLayout.PAGE_START);
	}
	
	
	// LOAD A FILE TO THE EVENTS EDITOR
	//--------------------------------------------------------------------------------------
	public void loadEventsFile() {
		
		int returnVal = fileChooser.showOpenDialog(null);
		
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			
			File fichero = fileChooser.getSelectedFile();
			
			try {
				
				String s = readFile(fichero);
				controller.reset();
				currentFile = fichero;
				eventsEditorPanel.setTexto(s);
				eventsEditorPanel.mySetBorder(currentFile.getName());
				statusBarPanel.setMessage("File " + fichero.getName() +
						 " of events loaded into the editor");
				
			}
			catch(FileNotFoundException e) {
				showsErrorDialog("Error reading the file: " +
						 e.getMessage());
			}
			
		}
		
	}
	
	// SAVE A FILE FROM THE EVENTS EDITOR
	//--------------------------------------------------------------------------------------
	public void saveEvents() {
		
		int returnVal = fileChooser.showSaveDialog(null);
		
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			
			File file = fileChooser.getSelectedFile();
			
			try {
				
				writeToFile(file, eventsEditorPanel.getTexto());
				currentFile = file;
				
				eventsEditorPanel.mySetBorder(currentFile.getName());
				statusBarPanel.setMessage("Events saved into the file " +
						 file.getName());
				
			}catch(IOException e) {
				showsErrorDialog("Error writting the file: " +
						 e.getMessage());
				
				
			}
			
		}
		
	}
	
	// SAVE REPORTS
	//--------------------------------------------------------------------------------------
	public void saveReports() {
		
		int returnVal = fileChooser.showSaveDialog(null);
		
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			
			File file = fileChooser.getSelectedFile();
			
			try {
				
				writeToFile(file, reportsPanel.getTexto());
				
				statusBarPanel.setMessage("Report saved into the file " +
						 file.getName());
				
			}catch(IOException e) {
				showsErrorDialog("Error writting the file: " +
						 e.getMessage());
			}
		}
			
	}
	
	// 	READ A TEXT FILE
	//--------------------------------------------------------------------------------------
	private String readFile(File file) throws FileNotFoundException {
		
		// Try with resources java8
		try(BufferedReader br = new BufferedReader(new FileReader(file)))
		{
			String text = "";
			String line = null;

			 while((line = br.readLine()) != null) {
			        text += line + "\n";       
			 }
			 return text;
			
		} catch (IOException e) {
			throw new FileNotFoundException();
		}
	}
	
	// 	WRITE IN A TEXT FILE
	//--------------------------------------------------------------------------------------
	private void writeToFile(File fichero, String content) throws IOException{
		// Try with resources java8
		// Resources are automatically closed when using try-with-resource block
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(fichero)))
		{
			bw.write(content);
	        bw.flush();	
		}
	}
	

	// 	EXECUTE SIMULATOR
	//--------------------------------------------------------------------------------------
	
	public void executeSimulator() {
	
		
		if(t == null) { // to press the botton one
			
			t = new Thread() { 
			
			public void run() {
				
				int i = 0;
				int N =  toolBar.getSimulatorSteps();
				int dlay = toolBar.getDelay();
			
				// Disable all the comoponents while in execution
				toolBar.disableToolbarComponents(); 
				menuBar.disabledMenus();
				
				while(!Thread.interrupted() && i < N) {
					
					try {
						controller.execute(1);
						Thread.sleep(dlay);
							
					}catch(InterruptedException e) {
						
						Thread.currentThread().interrupt(); 
					};
					
					i++;
				}

				// Enable components when the execution is finished
				toolBar.enableToolbarComponents();
				menuBar.enabledMenus();
				
		
				t = null; 
			}
			
			};
			
			// start thread
			t.start();
		}
	}
	
	// 	STOP SIMULATOR
	//--------------------------------------------------------------------------------------
	public void stopSimulator() {
		
		if(t != null) {
			t.interrupt();
		}
		
	}
	
	
	private void showsErrorDialog(String s) {
		JOptionPane.showMessageDialog(this,s, "Error" , JOptionPane.ERROR_MESSAGE);
	}
	
	public void setMensaje(String msg) {
		statusBarPanel.setMessage(msg);
	}
	
	public void clearEventsArea() {
		eventsEditorPanel.clearTextArea();
	}
	
	public void clearReportsArea() {
		reportsPanel.clearTextArea();;
	}
	
	public void insertInReportsArea(String texto) {
		reportsPanel.insertText(texto);
	}
	
	public String getEventsEditorText() {
		return eventsEditorPanel.getTexto();
	}
	
	public void insertInEventsEditor(String texto) {
		eventsEditorPanel.insertText(texto);
	}
	
	public int getStepsFromSimulator() {
		return  toolBar.getSimulatorSteps();
	}
	
	public int getSimulationTime() {
		return toolBar.getSimulationTime();
	}
	
	public void showReportsDialog() {
		reportsDialog.showDialog();
	}
	
	// ASKS IF YOU WANT TO EXIT THE APP
	//--------------------------------------------------------------------------------------
	public void exit() {
		
		int n = JOptionPane.showOptionDialog(this, "Are you sure you want to exit? ", "Exit",
		JOptionPane.YES_NO_OPTION,
		JOptionPane.QUESTION_MESSAGE, null, null, null);
		
		if (n == 0) System.exit(0);
	}

	
	// OBSERVERS
	//--------------------------------------------------------------------------------

	@Override
	public void simulatorError(int tiempo, RoadMap map, List<Event> events,
			SimulationError e) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				showsErrorDialog(e.getMessage());
			}
		});
		
	}

	@Override
	public void advance(int tiempo, RoadMap mapa, List<Event> events) {}

	@Override
	public void addEvent(int tiempo, RoadMap mapa, List<Event> events) {}

	@Override
	public void reset(int tiempo, RoadMap mapa, List<Event> events) {}
	
}
