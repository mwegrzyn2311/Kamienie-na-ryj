package classes.board;

import classes.elements.*;
import classes.gui.Scoreboard;
import classes.stuff.MoveDirection;
import classes.stuff.Vector2d;

import java.util.*;


public class MapLogic {
    private RectangularMap map;
    private List<IMapElement> movingElements = new LinkedList<>();
    private List<Vector2d> explodeNextTurn = new LinkedList<>();
    private List<Vector2d> explodeThisTurn = new LinkedList<>();
    private List<Vector2d> explosions = new LinkedList<>();
    public boolean gameOver = false;
    public boolean gameWon = false;
    public MapLogic(RectangularMap map){
        this.map = map;
    }

    public void printMoving(){
        System.out.println(movingElements.toString());
    }

    public void movePlayer(MoveDirection dir){
        if(this.map.player == null)
            return;
        Vector2d playerPosition = this.map.getPlayerPosition();
        Vector2d newPosition = playerPosition.add(dir.toUnitVector());
        if(map.canMoveTo(newPosition)){
            IMapElement ele = this.map.getElement(newPosition);
            if(ele instanceof Exit){
                gameWon = true;
            }
            if(ele instanceof GamePoint)
                getPoints(ele);

            this.map.setElement(playerPosition, new Empty(playerPosition));
            this.map.setElement(newPosition, this.map.player);
            startMovingElements(playerPosition);
        } else if(map.canPushAt(newPosition, dir)){
            Vector2d destination = newPosition.add(dir.toUnitVector());
            IMapElement empty = this.map.getElement(destination);
            IMapElement movable = this.map.getElement(newPosition);
            this.map.setElement(destination, movable);
            this.map.setElement(newPosition, this.map.player);
            this.map.setElement(playerPosition, empty);
            startMovingElements(playerPosition);
            startMovingPushedElement(destination);
        } else if(this.map.getElement(newPosition) instanceof Tunnel) {
            Tunnel tunnel = (Tunnel) this.map.getElement(newPosition);
            if(dir != tunnel.dir)
                return;
            Vector2d destination = newPosition.add(dir.toUnitVector());
            IMapElement destEle = this.map.getElement(destination);
            while(destEle instanceof Tunnel) {
                if(((Tunnel) destEle).dir != dir)
                    return;
                destination = destination.add(dir.toUnitVector());
                destEle = this.map.getElement(destination);
            }
            if(!this.map.canMoveTo(destination))
                return;
            IMapElement ele = this.map.getElement(destination);
            if(ele instanceof Exit){
                gameWon = true;
            }
            if(ele instanceof GamePoint)
                getPoints(ele);
            this.map.setElement(playerPosition, new Empty(playerPosition));
            this.map.setElement(destination, this.map.player);
            startMovingElements(playerPosition);
        }
    }

    public void eat(MoveDirection dir) {
        if(this.map.player == null)
            return;
        Vector2d playerPosition = this.map.getPlayerPosition();
        Vector2d newPosition = playerPosition.add(dir.toUnitVector());
        if(map.canMoveTo(newPosition)) {
            IMapElement ele = this.map.getElement(newPosition);
            if(ele instanceof Exit || ele instanceof Empty)
                return;

            if(ele instanceof GamePoint)
                getPoints(ele);

            this.map.setElement(newPosition, new Empty(newPosition));
            startMovingElements(newPosition);
        }
    }

    private void getPoints(IMapElement point) {
        this.map.player.points += ((GamePoint) point).pointValue;
        this.map.movableElements.remove(point);
        this.movingElements.remove(point);
        this.map.levelPanel.scoreboard.updateText();
    }

    public void startMovingPushedElement(Vector2d position){
        Vector2d below = position.add(new Vector2d(0,1));
        if(!this.map.isInMap(below))
            return;
        IMapElement ele = this.map.getElement(position);

        IMapElement belowElement = this.map.getElement(below);
        if(belowElement instanceof Empty){
            if(!this.movingElements.contains(ele))
                this.movingElements.add(ele);
            ele.startFalling();
            return;
        }
        if(belowElement.isMovable()) {
            IMapElement leftElement = this.map.getElement(position.add(new Vector2d(-1, 0)));
            IMapElement leftBelowElement = this.map.getElement(below.add(new Vector2d(-1,0)));
            if (leftElement instanceof Empty && leftBelowElement instanceof Empty){
                if(!this.movingElements.contains(ele))
                    this.movingElements.add(ele);
                ele.makeItWait();
                return;
            }
            IMapElement rightElement = this.map.getElement(position.add(new Vector2d(1, 0)));
            IMapElement rightBelowElement = this.map.getElement(below.add(new Vector2d(1,0)));
            if (rightElement instanceof Empty && rightBelowElement instanceof Empty){
                if(!this.movingElements.contains(ele))
                    this.movingElements.add(ele);
                ele.makeItWait();
            }
        }
    }

    // Function which is called whenever a tile becomes empty, so position argument is always instanceof Empty
    private void startMovingElements(Vector2d position){
        // Falling
        Vector2d above = position.add(new Vector2d(0,-1));
        IMapElement aboveElement = this.map.getElement(above);
        if(aboveElement != null && aboveElement.isMovable()) {
            if(!this.movingElements.contains(aboveElement))
                this.movingElements.add(aboveElement);
            aboveElement.startFalling();
        }
        // Rolling right or left
        Vector2d below = position.add(new Vector2d(0,1));
        if(this.map.getElement(below) instanceof Empty){
            Vector2d right = position.add(new Vector2d(1,0));
            if(this.map.isMovableElement(right) && this.map.isMovableElement(right.add(new Vector2d(0,1)))) {
                if(!this.movingElements.contains(this.map.getElement(right)))
                    this.movingElements.add(this.map.getElement(right));
            }
            Vector2d left = position.add(new Vector2d(-1,0));
            if(this.map.isMovableElement(left) && this.map.isMovableElement(left.add(new Vector2d(0,1)))) {
                if(!this.movingElements.contains(this.map.getElement(left)))
                    this.movingElements.add(this.map.getElement(left));
            }
        }
        // Rolling rightTop or leftTop
        if(aboveElement instanceof Empty){
            Vector2d rightTop = above.add(new Vector2d(1,0));
            if(this.map.isMovableElement(rightTop) && this.map.isMovableElement(rightTop.add(new Vector2d(0,1)))) {
                if(!this.movingElements.contains(this.map.getElement(rightTop)))
                    this.movingElements.add(this.map.getElement(rightTop));
                return;
            }
            Vector2d leftTop = above.add(new Vector2d(-1,0));
            if(this.map.isMovableElement(leftTop) && this.map.isMovableElement(leftTop.add(new Vector2d(0,1)))) {
                if(!this.movingElements.contains(this.map.getElement(leftTop)))
                    this.movingElements.add(this.map.getElement(leftTop));
            }
        }
    }


    public void moveElements(){
        Vector2d currentPosition;
        Vector2d belowPosition;
        Vector2d leftPosition;
        Vector2d rightPosition;
        IMapElement belowElement;
        IMapElement leftElement;
        IMapElement leftBelowElement;
        IMapElement rightElement;
        IMapElement rightBelowElement;

        Iterator<IMapElement> iter = this.map.movableElements.iterator();

        while(iter.hasNext()) {
            IMapElement movingElement = iter.next();
            if(!movingElements.contains(movingElement))
                continue;
            if(movingElement.isWaiting()) {
                continue;
            }
            currentPosition = movingElement.getPosition();
            belowPosition = currentPosition.add(new Vector2d(0, 1));
            belowElement = this.map.getElement(belowPosition);
            // Normal fall
            if(movingElement instanceof Disc && !(belowElement instanceof Empty || belowElement instanceof Explosion || belowElement.isFalling()) && movingElement.isFalling()) {
                kaboom(currentPosition);
                if (belowElement instanceof Disc) {
                    kaboom(belowPosition);
                }
            }
            else if ((belowElement instanceof Empty || belowElement instanceof Player || belowElement instanceof Disc) && movingElement.isFalling()) {
                if (belowElement instanceof Player)
                    killThePlayer();
                else if(belowElement instanceof Disc) {
                    kaboom(belowPosition);
                }
                else {
                    this.map.setElement(belowPosition, movingElement);
                    this.map.setElement(currentPosition, belowElement);
                    iter.remove();
                }
                if(belowElement instanceof Disc && !belowElement.isFalling()) {
                    kaboom(belowPosition);
                }
                //Stop moving faster than you might(an element that stop moving cannot move, so we can check that one step ahead))
                /*
                if(this.map.stopsMovingElement(belowPosition.add(new Vector2d(0,1))) && !(movingElement instanceof Disc)) {
                    movingElement.stopFalling();
                    this.movingElements.remove(movingElement);
                }

                 */

                startMovingElements(currentPosition);
            }// Now roll down
            else if(belowElement.isMovable()){
                leftPosition = currentPosition.add(new Vector2d(-1, 0));
                leftElement = this.map.getElement(leftPosition);
                leftBelowElement = this.map.getElement(leftPosition.add(new Vector2d(0, 1)));
                if (leftElement instanceof Empty && leftBelowElement instanceof Empty) {
                    this.map.setElement(leftPosition, movingElement);
                    this.map.setElement(currentPosition, leftElement);
                    movingElement.startFalling();
                    startMovingElements(currentPosition);
                    iter.remove();
                } else {
                    rightPosition = currentPosition.add(new Vector2d(1, 0));
                    rightElement = this.map.getElement(rightPosition);
                    rightBelowElement = this.map.getElement(rightPosition.add(new Vector2d(0, 1)));
                    if (rightElement instanceof Empty && rightBelowElement instanceof Empty) {
                        this.map.setElement(rightPosition, movingElement);
                        this.map.setElement(currentPosition,rightElement);
                        movingElement.startFalling();
                        startMovingElements(currentPosition);
                        iter.remove();
                    } else {
                        if(movingElement instanceof Disc)
                        System.out.println("Disc stops falling");
                        movingElement.stopFalling();
                        this.movingElements.remove(movingElement);
                    }
                }

            }
        }
        this.map.movableElements.addAll(movingElements);
    }

    private void kaboom(Vector2d position){
        if(!this.explodeNextTurn.contains(position))
            this.explodeNextTurn.add(position);
    }

    public void explosionsOver(){
        Iterator<Vector2d> iter = this.explosions.iterator();
        Vector2d position;
        while(iter.hasNext()){
            position = iter.next();
            this.map.setElement(position, new Empty(position));
            iter.remove();
        }
    }

    public void explodeEveryDisc(){
        explodeThisTurn = explodeNextTurn;
        explodeNextTurn = new LinkedList<>();
        Vector2d explodingPosition;
        IMapElement explodingElement;
        for(Vector2d position: explodeThisTurn){
            for(int i=-1; i<= 1; i++){
                for(int j = -1; j<= 1; j++){
                    explodingPosition = position.add(new Vector2d(i,j));
                    explodingElement = this.map.getElement(explodingPosition);
                    if(!explodingElement.isDestructible())
                        continue;
                    if(explodingElement instanceof Player) {
                        killThePlayer();
                    }
                    if(explodingElement instanceof Disc && (i != 0 || j != 0)) {
                        kaboom(explodingPosition);
                    }
                    if(explodingElement.isMovable()){
                        this.movingElements.remove(explodingElement);
                        this.map.movableElements.remove(explodingElement);
                    }
                    this.map.setElement(explodingPosition, new Explosion(explodingPosition));
                    if(!this.explosions.contains(explodingPosition))
                        this.explosions.add(explodingPosition);
                    startMovingElements(explodingPosition);
                }
            }
        }
    }

    private void killThePlayer(){
        if(this.map.player != null) {
            this.explodeNextTurn.add(this.map.getPlayerPosition());
            this.map.setElement(this.map.getPlayerPosition(), new Empty(this.map.getPlayerPosition()));
            this.map.player = null;
            this.gameOver = true;
        }
    }
}
