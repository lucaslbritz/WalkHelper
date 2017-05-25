package com.lucasbritz.walkhelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Created by Lucas Britz on 24/05/2017.
 */

public class BeaconRepository {

    public JsonObject createBeacon1() {
        JsonObject beaconData = beacon1();

        JsonArray array = new JsonArray();
        array.add(beacon2());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    public JsonObject createBeacon2() {
        JsonObject beaconData = beacon2();

        JsonArray array = new JsonArray();
        array.add(beacon1());
        array.add(beacon3());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    public JsonObject createBeacon3() {
        JsonObject beaconData = beacon3();

        JsonArray array = new JsonArray();
        array.add(beacon2());
        array.add(beacon4());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    public JsonObject createBeacon4() {
        JsonObject beaconData = beacon4();

        JsonArray array = new JsonArray();
        array.add(beacon3());
        array.add(beacon5());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    public JsonObject createBeacon5() {
        JsonObject beaconData = beacon5();

        JsonArray array = new JsonArray();
        array.add(beacon4());
        array.add(beacon6());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    public JsonObject createBeacon6() {
        JsonObject beaconData = beacon6();

        JsonArray array = new JsonArray();
        array.add(beacon5());
        array.add(beacon7());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    public JsonObject createBeacon7() {
        JsonObject beaconData = beacon7();

        JsonArray array = new JsonArray();
        array.add(beacon6());
        array.add(beacon8());
        array.add(beacon9());
        array.add(beacon10());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    public JsonObject createBeacon8() {
        JsonObject beaconData = beacon8();

        JsonArray array = new JsonArray();
        array.add(beacon7());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    public JsonObject createBeacon9() {
        JsonObject beaconData = beacon9();

        JsonArray array = new JsonArray();
        array.add(beacon7());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    public JsonObject createBeacon10() {
        JsonObject beaconData = beacon3();

        JsonArray array = new JsonArray();
        array.add(beacon7());

        beaconData.add("neighborhood", array);

        return beaconData;
    }

    private JsonObject beacon0() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "11111111-1111-1111-1111-111111111111");
        beaconData.addProperty("latitude", -29.793008);
        beaconData.addProperty("longitude", -51.152395);
        beaconData.addProperty("description", "Fábrica de Softwares");
        beaconData.addProperty("floorLevel", 2);
        beaconData.addProperty("active", true);

        return beaconData;
    }

    private JsonObject beacon1() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "1");
        beaconData.addProperty("latitude", -29.792233);
        beaconData.addProperty("longitude", -51.154587);
        beaconData.addProperty("description", "Unisinos. Acesso principal");
        beaconData.addProperty("floorLevel", 1);
        beaconData.addProperty("active", true);

        return beaconData;
    }

    private JsonObject beacon2() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "2");
        beaconData.addProperty("latitude", -29.792576);
        beaconData.addProperty("longitude", -51.154477);
        beaconData.addProperty("description", "B02");
        beaconData.addProperty("floorLevel", 1);
        beaconData.addProperty("active", true);

        return beaconData;
    }

    private JsonObject beacon3() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "3");
        beaconData.addProperty("latitude", -29.792732);
        beaconData.addProperty("longitude", -51.154429);
        beaconData.addProperty("description", "B03");
        beaconData.addProperty("floorLevel", 1);
        beaconData.addProperty("active", true);

        return beaconData;
    }

    private JsonObject beacon4() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "4");
        beaconData.addProperty("latitude", -29.792918);
        beaconData.addProperty("longitude", -51.154365);
        beaconData.addProperty("description", "B04");
        beaconData.addProperty("floorLevel", 1);
        beaconData.addProperty("active", true);

        return beaconData;
    }

    private JsonObject beacon5() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "5");
        beaconData.addProperty("latitude", -29.793078);
        beaconData.addProperty("longitude", -51.154331);
        beaconData.addProperty("description", "B05");
        beaconData.addProperty("floorLevel", 1);
        beaconData.addProperty("active", true);

        return beaconData;
    }

    private JsonObject beacon6() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "6");
        beaconData.addProperty("latitude", -29.793248);
        beaconData.addProperty("longitude", -51.154283);
        beaconData.addProperty("description", "B06");
        beaconData.addProperty("floorLevel", 1);
        beaconData.addProperty("active", true);

        return beaconData;
    }

    private JsonObject beacon7() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "7");
        beaconData.addProperty("latitude", -29.793767);
        beaconData.addProperty("longitude", -51.154152);
        beaconData.addProperty("description", "B07");
        beaconData.addProperty("floorLevel", 1);
        beaconData.addProperty("active", true);

        return beaconData;
    }

    private JsonObject beacon8() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "8");
        beaconData.addProperty("latitude", -29.793639);
        beaconData.addProperty("longitude", -51.153535);
        beaconData.addProperty("description", "auditório central");
        beaconData.addProperty("floorLevel", 1);
        beaconData.addProperty("active", true);

        return beaconData;
    }

    private JsonObject beacon9() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "9");
        beaconData.addProperty("latitude", -29.794247);
        beaconData.addProperty("longitude", -51.154771);
        beaconData.addProperty("description", "redondo");
        beaconData.addProperty("floorLevel", 1);
        beaconData.addProperty("active", true);

        return beaconData;
    }

    private JsonObject beacon10() {
        JsonObject beaconData = new JsonObject();
        beaconData.addProperty("id", "10");
        beaconData.addProperty("latitude", -29.795341);
        beaconData.addProperty("longitude", -51.153741);
        beaconData.addProperty("description", "corredor principal, próximo ao Fratélo");
        beaconData.addProperty("floorLevel", 1);
        beaconData.addProperty("active", true);

        return beaconData;
    }
}
