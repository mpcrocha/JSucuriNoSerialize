package examples.pathTracerSerial;

import java.util.ArrayList;

/**
 * Created by alexandrenery on 6/6/17.
 */
public class World {
    private ArrayList<Sphere> scene;

    public World()
    {
        scene = new ArrayList<Sphere>();
    }

    public void addSphere(Sphere s)
    {
        scene.add(s);
    }

    void initScene(){

        // left wall
        Sphere left_wall = new Sphere();
        left_wall.setRadius(200.0f);
        left_wall.setPosition(-200.6f, 0.0f, 0.0f);
        left_wall.setColor(0.75f, 0.25f, 0.25f);
        left_wall.setEmi(0.0f, 0.0f, 0.0f);
        addSphere(left_wall);

        // right wall
        Sphere right_wall = new Sphere();
        right_wall.setRadius(200.0f);
        right_wall.setPosition(200.6f, 0.0f, 0.0f);
        right_wall.setColor(0.25f, 0.25f, 0.75f);
        right_wall.setEmi(0.0f, 0.0f, 0.0f);
        addSphere(right_wall);

        // floor
        Sphere floor = new Sphere();
        floor.setRadius(200.0f);
        floor.setPosition(0.0f, -200.4f, 0.0f);
        floor.setColor(0.9f, 0.8f, 0.7f);
        floor.setEmi(0.0f, 0.0f, 0.0f);
        addSphere(floor);

        // ceiling
        Sphere ceil = new Sphere();
        ceil.setRadius(200.0f);
        ceil.setPosition(0.0f, 200.4f, 0.0f);
        ceil.setColor(0.9f, 0.8f, 0.7f);
        ceil.setEmi(0.0f, 0.0f, 0.0f);
        addSphere(ceil);

        // back wall
        Sphere back_wall = new Sphere();
        back_wall.setRadius(200.0f);
        back_wall.setPosition(0.0f, 0.0f, -200.4f);
        back_wall.setColor(0.9f, 0.8f, 0.7f);
        back_wall.setEmi(0.0f, 0.0f, 0.0f);
        addSphere(back_wall);

        // front wall
        Sphere front_wall = new Sphere();
        front_wall.setRadius(200.0f);
        front_wall.setPosition(0.0f, 0.0f, 202.0f);
        front_wall.setColor(0.9f, 0.8f, 0.7f);
        front_wall.setEmi(0.0f, 0.0f, 0.0f);
        addSphere(front_wall);

        // left sphere
        Sphere left = new Sphere();
        left.setRadius(0.16f);
        left.setPosition(-0.25f, -0.24f, -0.1f);
        left.setColor(0.9f, 0.8f, 0.7f);
        left.setEmi(0.0f, 0.0f, 0.0f);
        addSphere(left);

        // right sphere
        Sphere right = new Sphere();
        right.setRadius(0.2f);
        //right.setPosition(0.2f, -0.2f, 0.0f);
        right.setPosition(0.2f, 0.0f, 0.0f);
        right.setColor(0.9f, 0.8f, 0.7f);
        right.setEmi(0.0f, 0.0f, 0.0f);
        addSphere(right);

        // lightsource
        Sphere light = new Sphere();
        light.setRadius(1.0f);
        light.setPosition(0.0f, 1.36f, 0.0f);
        light.setColor(0.0f, 0.0f, 0.0f);
        light.setEmi(9.0f, 8.0f, 6.0f);
        addSphere(light);

    }

    public void updateScene()
    {
        //right sphere
        Sphere s = scene.get(7);

        //floor sphere
        Sphere floor = scene.get(2);

        Intersect it = floor.hit(s);
        if(it.hit)
        {
            //it.normal.mul(0.01f);
            //it.normal.x = 0.0f;
            //it.normal.z = 0.0f;
            //s.getVelocity().set(it.normal);
            s.getVelocity().invert();
        }

        s.getPosition().add(s.getVelocity());

        //if(s.getPosition().y > -0.2f)
        //    s.setPosition(s.getPosition().x,s.getPosition().y - 0.01f,s.getPosition().z);


    }


    Intersect hit(Ray ray)
    {
        //float inf = 1e20f;
        Intersect res;

        res = new Intersect();
        res.t = 1e20f;
        res.hit = false;

        for(int i = 0 ; i < scene.size() ; i++)
        {
            Sphere sphere = scene.get(i);

            Intersect it = sphere.hit(ray);

            //if(it.t != 0.0f && it.t < res.t) {
            if(it.hit && it.t < res.t){
                res.hit = it.hit;
                res.t = it.t;
                res.sphere_id = i;
                res.sphere = sphere;
                res.hitpoint = it.hitpoint;
                res.normal = it.normal;
                res.cosine_factor = it.cosine_factor;
            }
        }

        if(res.t < 1e20f && res.sphere != null)
            res.hit = true;

        return res;
    }

}