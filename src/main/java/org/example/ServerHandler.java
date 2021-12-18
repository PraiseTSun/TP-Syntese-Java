package org.example;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.FilenameUtils;
import org.example.Info.Building;
import org.example.Info.Civilisation;
import org.example.Info.Unit;

public class ServerHandler {
    enum TYPE{
        units,
        buildings,
        civs,
        ages
    }

    private final String EXT = "tab";

    private int serverPort;
    private Path dataFolder;

    private List<Unit> unitsList = new ArrayList<>();
    private List<Civilisation> civilisationList = new ArrayList<>();
    private List<Building> buildingList = new ArrayList<>();

    public void setUpServer(String[] arguments){
        argumentsHandler(arguments);
        readFiles();
        try {initServer();} catch (Exception e) {}
    }

    private void initServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 4);

        server.createContext("/test", exchange -> { exchangeTest(exchange);});
        server.createContext("/ages", exchange -> { exchangeAges(exchange);});

        server.start();
    }

    private void exchangeAges(HttpExchange exchange){
        String[] elements = exchange.getRequestURI().getPath().substring(1).split("/");

        try{
            if(elements.length == 1){
                exchange.sendResponseHeaders(202,0);
                writeBuffer(getAgesInfo(TYPE.ages),exchange);
            }
            else if(elements.length >= 3){
                if(elements[1].equals(":age")){
                    if(elements[2].equals(TYPE.units.toString())){
                        if(elements.length >= 4){
                            String[] target = elements[3].split("=");
                            if(target.length == 2 && target[0].equals("civ")){
                                exchange.sendResponseHeaders(202, 0);
                                writeBuffer(getTargetAges(target[1], TYPE.units), exchange);
                            } else exchange.sendResponseHeaders(404, 0);
                        }
                        else{
                            exchange.sendResponseHeaders(202, 0);
                            writeBuffer(getAgesInfo(TYPE.units), exchange);
                        }
                    }
                    else if(elements[2].equals(TYPE.buildings.toString())){
                        if(elements.length >= 4){
                            String[] target = elements[3].split("=");
                            if(target.length == 2 && target[0].equals("civ")){
                                exchange.sendResponseHeaders(202, 0);
                                writeBuffer(getTargetAges(target[1], TYPE.buildings), exchange);
                            } else exchange.sendResponseHeaders(404, 0);
                        }
                        else{
                            exchange.sendResponseHeaders(202, 0);
                            writeBuffer(getAgesInfo(TYPE.buildings), exchange);
                        }
                    } else exchange.sendResponseHeaders(404, 0);
                } else exchange.sendResponseHeaders(404, 0);;
            } else exchange.sendResponseHeaders(404, 0);
        } catch (IOException e) {}
        finally{ exchange.close(); }
    }

    private String getAgesInfo(TYPE type){
        return getAgesInfo(type, "");
    }

    private String getAgesInfo(TYPE type, String target){
        String info = "";

        switch(type){
            case ages:
                info += getAgesInfo(TYPE.units);
                info += "\n\n";
                info += getAgesInfo(TYPE.buildings);
                break;
            case units:
                info += "                           Units at age                            \n";
                info += "----------------------------------------------------------------------\n";
                for(Unit unit : unitsList){
                    info += unit.getName() + " : " + getAges(unit.getAllAges()) + "\n";
                }
                break;
            case buildings:
                info += "                         Buildings at age                          \n";
                info += "----------------------------------------------------------------------\n";
                for(Building building : buildingList){
                    info += building.getName() + " : " + getAges(building.getAges()) + "\n";
                }
                break;
        }

        return info;
    }

    private String getTargetAges(String target, TYPE type){
        String info = "";
        String title = "";
        switch(type){
            case units:
                title = target + "'s units at age\n--------------------------------------------\n";
                for (Unit unit : unitsList) {
                    for(String civ : unit.getCivilisations())
                        if(civ.equals(target)){
                            info += unit.getName() + " : " + getAges(unit.getAllAges()) + "\n";
                        }
                }
                break;
            case buildings:
                title = target + "'s buildings at age\n--------------------------------------------\n";
                for (Building build : buildingList) {
                    for(String civ : build.getCivilisations())
                        if(civ.equals(target)){
                            info += build.getName() + " : " + getAges(build.getAges()) + "\n";
                        }
                }
                break;
        }
        if( info == "")
            info = target + " do not exist.";

        return title + info;
    }

    private String getAges(String[] ages){
        String age = "";
        for(int i = 0; i < ages.length; i++){
            switch(ages[i]){
                case "1": age += "Dark Age"; break;
                case "2": age += "Feudal Age"; break;
                case "3": age += "Castle Age"; break;
                case "4": age += "Imperial Age"; break;
            }

            if(i != ages.length-1)
                age += " | ";
        }
        return age;
    }

    private void exchangeTest(HttpExchange exchange){
        try {
            exchange.sendResponseHeaders(200, 0);
            writeBuffer("test\ntest", exchange);
        } catch (IOException e) {}
        finally {
            exchange.close();
        }
    }

    private void writeBuffer(String content, HttpExchange exchange){
        try{
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody()));
            w.write(content);
            w.flush();
        } catch (IOException e) {}
    }

    private void argumentsHandler(String[] arguments){
        if(arguments.length < 7)
            throw new RuntimeException("There are some missing arguments");

        serverPort = Integer.parseInt(arguments[2]);

        if(!Files.isDirectory(Paths.get(arguments[4])))
            throw new RuntimeException(arguments[4] + " is not the data folder path.");
        dataFolder = Paths.get(arguments[4]);
    }

    private void readFiles(){
        File folder = new File(dataFolder.toString());
        File[] listOfFiles = folder.listFiles();

        for ( File file : listOfFiles) {
            if(acceptedFile(file))
                collectData(file);
        }
    }

    private void collectData(File file){
        try(BufferedReader reader = Files.newBufferedReader(Paths.get(file.getPath()))){
            String line;
            String fileName = file.getName();
            String[] lineElements;
            while((line = reader.readLine()) != null){
                lineElements = line.split("\t");
                switch (fileName){
                    case "civs.tab":
                        civilisationList.add(new Civilisation(lineElements[0], lineElements[1], lineElements[2], lineElements[3], lineElements[4]));
                        break;
                    case "buildings.tab":
                        buildingList.add(new Building(lineElements[0], lineElements[1].split(" "), lineElements[2], lineElements[3].split(" "), lineElements[4], Integer.parseInt(lineElements[5]), Integer.parseInt(lineElements[6]), lineElements[7].split(" ")));
                        break;
                    case "units.tab":
                        unitsList.add(new Unit(lineElements[0], lineElements[1].split(" "), lineElements[2], lineElements[3].split(" "), lineElements[4], Integer.parseInt(lineElements[5]), Integer.parseInt(lineElements[6]), lineElements[7].split(" ")));
                        break;
                }
            }
        } catch (IOException e) {}
    }

    private boolean acceptedFile(File file){
        return file.isFile() && FilenameUtils.getExtension(file.getName()).equals(EXT);
    }

}
