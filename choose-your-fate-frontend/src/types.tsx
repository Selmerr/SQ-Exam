export interface scene {
    id: number
    dialog: Array<string>
    choices: Array<choice>
    img: string
}

export interface choice {
    id: number
    name: string
    destination_id: number    
}

export interface IProps {
    testMethod?: Function
}

export interface IState {
    currentScene: number
}
