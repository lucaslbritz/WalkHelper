package com.lucasbritz.walkhelper;

import java.util.ArrayList;

/**
 * Created by Lucas Britz on 24/05/2017.
 */

public class BeaconRepository {

    public ArrayList<Beacon> findAllBeacons() {
        ArrayList<Beacon> beaconList = new ArrayList<>();
        beaconList.add(createBeacon1());
        beaconList.add(createBeacon2());
        beaconList.add(createBeacon3());
        beaconList.add(createBeacon4());
        beaconList.add(createBeacon5());
        beaconList.add(createBeacon6());
        beaconList.add(createBeacon7());
        beaconList.add(createBeacon8());
        beaconList.add(createBeacon9());
        beaconList.add(createBeacon10());

        return beaconList;
    }

    public Beacon findBeaconByAddress(String address) {
        ArrayList<Beacon> beaconList = findAllBeacons();

        return beaconList.stream()
                .filter(b -> b.getAddress().equalsIgnoreCase(address))
                .findFirst()
                .get();
    }

    private Beacon createBeacon1() {
        Beacon beacon = beacon1();

        ArrayList<String> neighborhood = new ArrayList<>();
        neighborhood.add(beacon2().getAddress());

        beacon.setNeighborhood(neighborhood);

        return beacon;
    }

    private Beacon createBeacon2() {
        Beacon beacon = beacon2();

        ArrayList<String> neighborhood = new ArrayList<>();
        neighborhood.add(beacon1().getAddress());
        neighborhood.add(beacon3().getAddress());

        beacon.setNeighborhood(neighborhood);

        return beacon;
    }

    private Beacon createBeacon3() {
        Beacon beacon = beacon3();

        ArrayList<String> neighborhood = new ArrayList<>();
        neighborhood.add(beacon2().getAddress());
        neighborhood.add(beacon4().getAddress());

        beacon.setNeighborhood(neighborhood);

        return beacon;
    }

    private Beacon createBeacon4() {
        Beacon beacon = beacon4();

        ArrayList<String> neighborhood = new ArrayList<>();
        neighborhood.add(beacon3().getAddress());
        neighborhood.add(beacon5().getAddress());

        beacon.setNeighborhood(neighborhood);

        return beacon;
    }

    private Beacon createBeacon5() {
        Beacon beacon = beacon5();

        ArrayList<String> neighborhood = new ArrayList<>();
        neighborhood.add(beacon4().getAddress());
        neighborhood.add(beacon6().getAddress());

        beacon.setNeighborhood(neighborhood);

        return beacon;
    }

    private Beacon createBeacon6() {
        Beacon beacon = beacon6();

        ArrayList<String> neighborhood = new ArrayList<>();
        neighborhood.add(beacon5().getAddress());
        neighborhood.add(beacon7().getAddress());

        beacon.setNeighborhood(neighborhood);

        return beacon;
    }

    private Beacon createBeacon7() {
        Beacon beacon = beacon7();

        ArrayList<String> neighborhood = new ArrayList<>();
        neighborhood.add(beacon6().getAddress());
        neighborhood.add(beacon8().getAddress());
        neighborhood.add(beacon9().getAddress());
        neighborhood.add(beacon10().getAddress());

        beacon.setNeighborhood(neighborhood);

        return beacon;
    }

    private Beacon createBeacon8() {
        Beacon beacon = beacon8();

        ArrayList<String> neighborhood = new ArrayList<>();
        neighborhood.add(beacon7().getAddress());

        beacon.setNeighborhood(neighborhood);

        return beacon;
    }

    private Beacon createBeacon9() {
        Beacon beacon = beacon9();

        ArrayList<String> neighborhood = new ArrayList<>();
        neighborhood.add(beacon7().getAddress());

        beacon.setNeighborhood(neighborhood);

        return beacon;
    }

    private Beacon createBeacon10() {
        Beacon beacon = beacon10();

        ArrayList<String> neighborhood = new ArrayList<>();
        neighborhood.add(beacon7().getAddress());

        beacon.setNeighborhood(neighborhood);

        return beacon;
    }

    private Beacon beacon1() {
        Beacon beacon = new Beacon();
        beacon.setAddress("01:01:01:01:01:01");
        beacon.setLatitude(-29.792233);
        beacon.setLongitude(-51.154587);
        beacon.setDescription("B1");
        beacon.setFloorLevel(1);

        return beacon;
    }

    private Beacon beacon2() {
        Beacon beacon = new Beacon();
        beacon.setAddress("02:02:02:02:02:02");
        beacon.setLatitude(-29.792576);
        beacon.setLongitude(-51.154477);
        beacon.setDescription("B2");
        beacon.setFloorLevel(1);

        return beacon;
    }

    private Beacon beacon3() {
        Beacon beacon = new Beacon();
        beacon.setAddress("03:03:03:03:03:03");
        beacon.setLatitude(-29.792732);
        beacon.setLongitude(-51.154429);
        beacon.setDescription("B3");
        beacon.setFloorLevel(1);

        return beacon;
    }

    private Beacon beacon4() {
        Beacon beacon = new Beacon();
        beacon.setAddress("04:04:04:04:04:04");
        beacon.setLatitude(-29.792918);
        beacon.setLongitude(-51.154365);
        beacon.setDescription("B4");
        beacon.setFloorLevel(1);

        return beacon;
    }

    private Beacon beacon5() {
        Beacon beacon = new Beacon();
        beacon.setAddress("05:05:05:05:05:05");
        beacon.setLatitude(-29.793078);
        beacon.setLongitude(-51.154331);
        beacon.setDescription("B5");
        beacon.setFloorLevel(1);

        return beacon;
    }

    private Beacon beacon6() {
        Beacon beacon = new Beacon();
        beacon.setAddress("06:06:06:06:06:06");
        beacon.setLatitude(-29.793248);
        beacon.setLongitude(-51.154283);
        beacon.setDescription("B6");
        beacon.setFloorLevel(1);

        return beacon;
    }

    private Beacon beacon7() {
        Beacon beacon = new Beacon();
        beacon.setAddress("D3:8A:63:6D:FB:79");
        beacon.setLatitude(-29.793767);
        beacon.setLongitude(-51.154152);
        beacon.setDescription("B7");
        beacon.setFloorLevel(1);

        return beacon;
    }

    private Beacon beacon8() {
        Beacon beacon = new Beacon();
        beacon.setAddress("08:08:08:08:08:08");
        beacon.setLatitude(-29.793639);
        beacon.setLongitude(-51.153535);
        beacon.setDescription("auditório central");
        beacon.setFloorLevel(1);

        return beacon;
    }

    private Beacon beacon9() {
        Beacon beacon = new Beacon();
        beacon.setAddress("09:09:09:09:09:09");
        beacon.setLatitude(-29.794247);
        beacon.setLongitude(-51.154771);
        beacon.setDescription("redondo");
        beacon.setFloorLevel(1);

        return beacon;
    }

    private Beacon beacon10() {
        Beacon beacon = new Beacon();
        beacon.setAddress("10:10:10:10:10:10");
        beacon.setLatitude(-29.795341);
        beacon.setLongitude(-51.153741);
        beacon.setDescription("corredor principal, próximo ao Fratelo");
        beacon.setFloorLevel(1);

        return beacon;
    }
}
