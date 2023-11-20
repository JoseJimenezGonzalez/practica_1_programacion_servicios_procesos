package restaurante;

import java.sql.Array;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class Restaurante {
    static Semaphore[] semaforoMostrador = {new Semaphore(1), new Semaphore(1)};
    // 0 -> Pizza
    // 1 -> Bocadillo
    static int numeroComidaMostrador[] = new int[2];
    static int totalComidaVendida[] = new int[2];
    // 0 -> Pizza
    // 1 -> Bocadillo
    static Semaphore recaudacion = new Semaphore(1);
    static int totalClientes = 0;
    final static int precioBocadillo = 6;
    final static int precioPizza = 12;
    static int totalDineroRecaudado = 0;
    static boolean restauranteAbierto = true;
    static int idCliente = 0;
    public static void cerrarRestaurante() {
        restauranteAbierto = false;
    }

    public static class Pizzero extends Thread{
        @Override
        public void run() {
            while (restauranteAbierto){
                try {
                    //Estirar la masa, son 2 segundos
                    System.out.println("Pizzero estirando la masa");
                    Thread.sleep(2000);
                    //Poner los ingredientes, es 1 segundo
                    System.out.println("Pizzero añadiendo los ingredientes");
                    Thread.sleep(1000);
                    //Cocinar la pizza, son 5 segundos
                    System.out.println("Pizzero cocinando la pizza");
                    Thread.sleep(5000);
                    //Incremento la cantidad de pizzas disponibles en mostrador
                    Restaurante.semaforoMostrador[0].acquire();
                    Restaurante.numeroComidaMostrador[0]++;
                    System.out.println("Hay una pizza lista, rápido que se enfría");
                    System.out.println("En el mostrador hay " + numeroComidaMostrador[0] + " pizzas");
                    Restaurante.semaforoMostrador[0].release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static class Bocatero extends Thread{
        @Override
        public void run() {
            while (restauranteAbierto){
                try {
                    //Cortar pan, son 2 segundos
                    System.out.println("El bocatero está cortando el pan");
                    Thread.sleep(2000);
                    //Poner los ingredientes, es 1 segundo
                    System.out.println("Bocatero añadiendo los ingredientes");
                    Thread.sleep(1000);
                    //Calentar el bocata, son 5 segundos
                    System.out.println("Bocatero calentado el bocadillo");
                    Thread.sleep(5000);
                    //Incremento la cantidad de bocatas disponibles en mostrador
                    Restaurante.semaforoMostrador[1].acquire();
                    Restaurante.numeroComidaMostrador[1]++;
                    System.out.println("Hay un bocata listo, rápido que se enfría");
                    System.out.println("En el mostrador hay " + numeroComidaMostrador[1] + " bocatas");
                    Restaurante.semaforoMostrador[1].release();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public static class Cliente extends Thread{
        Random random = new Random();
        int numeroAleatorio = random.nextInt(2);
        int cantidadAleatoriaDeProducto = random.nextInt(5) + 1;
        String tipo;
        int idTipo;
        int idCliente;
        int cantidadDeseada;
        int cantidadDeseadaAux;
        boolean atendido;
        int precioTotal;
        Cliente(){
            this.idCliente = Restaurante.idCliente++;
            idTipo = numeroAleatorio;
            cantidadDeseada = cantidadAleatoriaDeProducto;
            cantidadDeseadaAux = cantidadAleatoriaDeProducto;
            atendido = false;
            switch (numeroAleatorio){
                case 0:
                    tipo = "Pizza";
                    precioTotal = precioPizza * cantidadDeseada;
                    break;
                case 1:
                    tipo = "Bocadillo";
                    precioTotal = precioBocadillo * cantidadDeseada;
                    break;
                default:
                    throw new AssertionError();
            }
        }
        @Override
        public void run() {
            try{
                System.out.println("Estoy pensando...");//Ya piensa despues 10 segundos
                System.out.println("Quiero " + cantidadDeseada + " de " + tipo + ".");
                    while (!atendido){
                        Thread.sleep(5000);
                        semaforoMostrador[idTipo].acquire();
                        //Opciones
                        if(numeroComidaMostrador[idTipo] > cantidadDeseadaAux){
                            numeroComidaMostrador[idTipo] -= cantidadDeseadaAux;
                            totalComidaVendida[idTipo] += cantidadDeseadaAux;
                            System.out.println("El cliente con id: " + idCliente + " ha cogido " + cantidadDeseadaAux + " de " + tipo);
                            atendido = true;
                        }else if(numeroComidaMostrador[idTipo] == cantidadDeseadaAux){
                            totalComidaVendida[idTipo] += cantidadDeseadaAux;
                            numeroComidaMostrador[idTipo] = 0;
                            System.out.println("El cliente con id: " + idCliente + " ha cogido " + cantidadDeseadaAux + " de " + tipo);
                            atendido = true;
                        }else if(numeroComidaMostrador[idTipo] == 0){
                            System.out.println("El cliente con id: " + idCliente + " no ha cogido ningun/a " + tipo + " porque no hay.");
                        }else{
                            cantidadDeseadaAux -= numeroComidaMostrador[idTipo];
                            totalComidaVendida[idTipo] += numeroComidaMostrador[idTipo];
                            numeroComidaMostrador[idTipo] = 0;
                            System.out.println("El cliente con id: " + idCliente + " ha cogido " + cantidadDeseadaAux + " de " + tipo +". Todavía le falta por coger, va a esperar hasta que se reponga el stock.");
                        }
                        System.out.println("Quedan " + numeroComidaMostrador[idTipo] + " de " + tipo);
                        semaforoMostrador[idTipo].release();
                    }
                    recaudacion.acquire();
                    System.out.println("El cliente con id: " + idCliente + " compró " + cantidadDeseada + " de " + tipo);
                    System.out.println("El cliente ha pagado: " + precioTotal + " euros");
                    System.out.println("El cliente se despide");
                    totalDineroRecaudado += precioTotal;
                    recaudacion.release();
            }catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }
    }
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // Primero bocateros y pizzeros
        Pizzero pizzero = new Pizzero();
        Bocatero bocatero = new Bocatero();
        // Solicitar al usuario la cantidad de clientes
        System.out.println("Ingrese la cantidad de clientes que entrarán al restaurante: ");
        totalClientes = scanner.nextInt();
        // Ponemos en marcha a los cocineros
        pizzero.start();
        bocatero.start();
        // Crear hilos de clientes y los ponemos en marcha
        Cliente[] clientes = new Cliente[totalClientes];
        for (int i = 0; i < totalClientes; i++) {
            clientes[i] = new Cliente();
            clientes[i].start();
        }
        for (Cliente cliente : clientes) {
            try {
                cliente.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        cerrarRestaurante();
        try {
            pizzero.join();
            bocatero.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Imprimir estadísticas al final del día
        System.out.println("Cierre del restaurante");
        System.out.println("Total de pizzas vendidas: " + totalComidaVendida[0]);
        System.out.println("Total de bocadillos vendidos: " + totalComidaVendida[1]);
        System.out.println("Total de dinero recaudado: " + totalDineroRecaudado + " euros");
    }
}
