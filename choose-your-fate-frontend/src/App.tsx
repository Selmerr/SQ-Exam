import './App.css';
import { ShowDialog } from './ShowDialog';
import { IProps, IState, scene } from './types';
import { Component } from 'react';

export class App extends Component<IProps, IState> {
  constructor(props: IProps) {
    super(props);

    this.state = {
      currentScene: 0,
    };
  }

  scenes: Array<scene> =[
    { 
      id: 0, 
      choices: [
        { id: 0, name: "Go left", destination_id: 2 },
        { id: 1, name: "Go right", destination_id: 1 }
      ],
      dialog: ["Dette er tekst\n", "Men det kunne også være noget andet\n", "eeeeeellleer, måske noget helt tredje."],
      img: "images/Welcome.png"
    },
    { 
      id: 1, 
      choices: [
        { id: 0, name: "leave", destination_id: 0 },
      ],
      dialog: ["You went left, good for you.", "...", "please leave"],
      img: "images/Show.png"
    },
    { 
      id: 1, 
      choices: [
        { id: 0, name: "leave", destination_id: 0 },
      ],
      dialog: ["This is what we call the illution of choice", "...", "please leave"],
      img: "images/Show.png"
    }
  ]

  
  changeScene(nextScene: number) {
    this.setState(() => ({
      currentScene: nextScene,
    }));
  }

  render() {
    return ShowDialog(this.scenes[this.state.currentScene], this.changeScene.bind(this));
  }
}