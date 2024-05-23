package quickview.backend

class TestinaController {
    static responseFormats = ['json', 'xml']

    def index() {
        [
                ciccio     : 'pallo',
                testinaList: Testina.list()
        ]
    }

    def create() {
        Random random = new Random();

        def a1 = new Testina(name: 'Ciccio' + random.nextInt(10), eight: 1.43, gender: 'male').save()
        def a2 = new Testina(name: 'Paperino' + random.nextInt(10), eight: 1.12, gender: 'female').save()

        [
                firstX : a1,
                secondX: a2
        ]
    }
}
