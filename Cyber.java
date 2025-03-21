package Cyber;

import robocode.util.Utils;
import java.util.HashMap;
import robocode.*;
import java.awt.Color;
import java.util.Random;

/**
 * Cyber - um robô criado por (Sabrina Aquino, Ana Clara e Rafael Oliveira)
 */
public class Cyber extends AdvancedRobot {

    private double scanDir = 1; // Direção do radar (1 ou -1)
    private HashMap<String, RobotData> enemyMap = new HashMap<>(); // Mapa de inimigos
    private Random rand = new Random(); // Instância para gerar números aleatórios

    /**
     * run: Comportamento padrão do Cyber
     */
    public void run() {
        setBodyColor(Color.darkGray); // Cor do chassi/corpo
        setGunColor(Color.black); // Cor do canhão
        setRadarColor(Color.lightGray); // Cor do radar
        setBulletColor(Color.white); // Cor da bala
        setScanColor(Color.green); // Cor do Scan

        // Loop principal do robô
        while (true) {
            moveAlongEdges(); // Move ao longo das bordas
            setTurnRadarRight(360 * scanDir); // Gira o radar a 360 graus

            // Movimento aleatório
            if (rand.nextBoolean()) {
                setAhead(100 + rand.nextInt(200)); // Movimenta-se aleatoriamente
                setTurnRight(rand.nextInt(180) - 90); // Gira aleatoriamente
            } else {
                setAhead(200); // Avança mais rápido
                setTurnRight(45 + rand.nextInt(90)); // Gira de forma aleatória
            }

            execute();
        }
    }

    /**
     * moveAlongEdges: Faz o robô se mover ao longo das bordas sem bater.
     */
    private void moveAlongEdges() {
        int borderRange = 100;

        // Verifica se o robô está perto das bordas
        if (getX() < borderRange || getX() > getBattleFieldWidth() - borderRange ||
            getY() < borderRange || getY() > getBattleFieldHeight() - borderRange) {
            setAhead(100); // Move para frente
            setTurnRight(rand.nextInt(180) - 90); // Gira aleatoriamente para evitar colisão
        }
    }

    /**
     * handleRadar: Gerencia a movimentação do radar
     */
    private void handleRadar() {
        // Aumenta a área de detecção do radar
        setTurnRadarRight(360 * scanDir);
        
        if (getRadarTurnRemaining() == 0) { 
            scanDir = -scanDir; // Inverte a direção do radar
        }
    }

    /**
     * onScannedRobot: O que fazer quando avistar outro robô
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        String robotank = e.getName();
        double distancia = e.getDistance();
        
        // Atualiza os dados do inimigo
        updateEnemyMap(e);

        // Ajusta o disparo com base na distância
        if (distancia < 135) {
            fire(3);  // Maior potência quando o inimigo está muito perto
        } else if (distancia < 300) {
            fire(2);  // Potência média para inimigos a uma distância média
        } else {
            fire(1);  // Menor potência para inimigos distantes
        }

        // Chama o método para manobrar e disparar
        handleGun(e);
    }

    /**
     * updateEnemyMap: Atualiza as informações dos inimigos
     */
    private void updateEnemyMap(ScannedRobotEvent e) {
        final String scannedRobotName = e.getName();

        // Verifica se o inimigo já está no mapa
        RobotData scannedRobot = enemyMap.get(scannedRobotName);
        
        if (scannedRobot == null) {
            scannedRobot = new RobotData(e);
            enemyMap.put(scannedRobotName, scannedRobot);
        } else {
            scannedRobot.update(e);
        }

        // Ajusta a direção do radar com base no comportamento do inimigo
        updateScanDirection(e);
    }

    // Método para atualizar a direção do radar
    private void updateScanDirection(ScannedRobotEvent e) {
        if (e.getDistance() < 200) {
            scanDir = -scanDir; // Inverte a direção do radar se inimigo estiver muito perto
        }
    }

    /**
     * handleGun: Calcula o disparo em função da posição e movimento do alvo
     */
    private void handleGun(ScannedRobotEvent e) {
        double FIREPOWER = 1; // Valor da potência de disparo
        double bulletSpeed = 20 - FIREPOWER * 3; // Velocidade da bala

        double angleToEnemy = getHeadingRadians() + e.getBearingRadians(); // Ângulo para o inimigo
        double enemyX = getX() + e.getDistance() * Math.sin(angleToEnemy); // Posição X do inimigo
        double enemyY = getY() + e.getDistance() * Math.cos(angleToEnemy); // Posição Y do inimigo

        // Prediz a posição futura do inimigo com base em sua velocidade e direção
        double time = e.getDistance() / bulletSpeed;
        double predictedX = enemyX + e.getVelocity() * Math.sin(e.getHeading()) * time;
        double predictedY = enemyY + e.getVelocity() * Math.cos(e.getHeading()) * time;

        // Aponta o canhão para a posição prevista do alvo
        setTurnGunRightRadians(Utils.normalRelativeAngle(Math.atan2(predictedX - getX(), predictedY - getY()) - getGunHeadingRadians()));

        // Dispara com a potência definida
        fire(FIREPOWER);
    }

    /**
     * onHitByBullet: O que fazer quando for atingido por uma bala
     */
    public void onHitByBullet(HitByBulletEvent e) {
        // Adicionar lógica para evasão ou contra-ataque
        setAhead(50); // Move-se para frente
        setTurnRight(90); // Gira 90 graus
    }

    /**
     * onHitWall: O que fazer quando bater em uma parede
     */
    public void onHitWall(HitWallEvent e) {
        // Desvia após ser atingido
        setTurnRight(90); // Gira 90 graus
        setAhead(50); // Avança 50 unidades
    }

    // Classe RobotData para armazenar informações sobre o inimigo
    public class RobotData {
        private double lastDistance;
        private double lastHeading;
        private double lastVelocity;

        public RobotData(ScannedRobotEvent e) {
            this.lastDistance = e.getDistance();
            this.lastHeading = e.getHeading();
            this.lastVelocity = e.getVelocity();
        }

        public void update(ScannedRobotEvent e) {
            this.lastDistance = e.getDistance();
            this.lastHeading = e.getHeading();
            this.lastVelocity = e.getVelocity();
        }

        public double getLastDistance() {
            return lastDistance;
        }

        public double getLastHeading() {
            return lastHeading;
        }

        public double getLastVelocity() {
            return lastVelocity;
        }
    }
}